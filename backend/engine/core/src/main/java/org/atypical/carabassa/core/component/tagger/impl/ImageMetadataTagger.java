package org.atypical.carabassa.core.component.tagger.impl;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import jakarta.annotation.PostConstruct;

@Component
public class ImageMetadataTagger extends GenericMetadataTagger {

    private static final Logger logger = LoggerFactory.getLogger(ImageMetadataTagger.class);

    @Value("${carabassa.default-tz}")
    private String defaultTimeZone;

    @Value("${carabassa.tempdir:#{null}}")
    private String tempDirLocation;

    @PostConstruct
    public void init() {
        if (tempDirLocation != null) {
            ImageIO.setCacheDirectory(Paths.get(tempDirLocation).toFile());
        }
    }

    @Override
    public Set<Tag> getTags(Resource inputItem) throws IOException {
        Metadata metadata = super.getMetaData(inputItem);
        Set<Tag> tags = super.getTags(inputItem, metadata);

        if (metadata != null) {
            tags.addAll(getCustomImageTags(metadata));
        }

        if (logger.isTraceEnabled()) {
            super.printTags(tags);
        }

        return tags;
    }

    private Set<Tag> getCustomImageTags(Metadata metadata) throws IOException {
        Set<Tag> tags = new HashSet<>();

        Instant archiveTime = getArchiveTime(metadata);
        if (archiveTime != null) {
            tags.add(new TagImpl(TAG_ARCHIVE_TIME, archiveTime));
        }

        return tags;
    }

    private Instant getArchiveTime(Metadata metadata) {
        TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of(defaultTimeZone));
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (directory != null) {
            Date dateOriginal = directory.getDateOriginal(timeZone);
            if (dateOriginal != null) {
                return dateOriginal.toInstant();
            }
        }
        return null;
    }

}
