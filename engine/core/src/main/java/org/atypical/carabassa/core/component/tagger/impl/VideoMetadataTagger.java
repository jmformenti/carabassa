package org.atypical.carabassa.core.component.tagger.impl;

import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.drew.metadata.Metadata;
import com.drew.metadata.mp4.Mp4Directory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

@Component
public class VideoMetadataTagger extends GenericMetadataTagger {

    private static final Logger logger = LoggerFactory.getLogger(VideoMetadataTagger.class);

    @Value("${carabassa.default-tz}")
    private String defaultTimeZone;

    @Value("${carabassa.tempdir:#{null}}")
    private String tempDirLocation;

    @Override
    public Set<Tag> getTags(Resource inputItem) throws IOException {
        File inputFile = getFile(inputItem);

        Metadata metadata = super.getMetaData(new FileSystemResource(inputFile));
        Set<Tag> tags = super.getTags(inputItem, metadata);

        if (metadata != null) {
            tags.addAll(getCustomVideoTags(metadata));
        }

        if (logger.isTraceEnabled()) {
            super.printTags(tags);
        }

        releaseFile(inputItem, inputFile);

        return tags;
    }

    private Set<Tag> getCustomVideoTags(Metadata metadata) {
        Set<Tag> tags = new HashSet<>();

        Instant archiveTime = getArchiveTime(metadata);
        if (archiveTime != null) {
            tags.add(new TagImpl(TAG_ARCHIVE_TIME, archiveTime));
        }

        return tags;
    }

    private Instant getArchiveTime(Metadata metadata) {
        TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of(defaultTimeZone));
        Mp4Directory directory = metadata.getFirstDirectoryOfType(Mp4Directory.class);
        if (directory != null) {
            Date dateOriginal = directory.getDate(Mp4Directory.TAG_CREATION_TIME, timeZone);
            if (dateOriginal != null) {
                return dateOriginal.toInstant();
            } else {
                Date dateModified = directory.getDate(Mp4Directory.TAG_MODIFICATION_TIME, timeZone);
                if (dateModified != null) {
                    return dateModified.toInstant();
                }
            }
        }
        return null;
    }

    private File getFile(Resource inputItem) throws IOException {
        if (inputItem.isFile()) {
            return inputItem.getFile();
        } else {
            File tempFile;
            if (tempDirLocation != null) {
                tempFile = File.createTempFile("vid", null, Paths.get(tempDirLocation).toFile());
            } else {
                tempFile = File.createTempFile("vid", null);
            }
            tempFile.deleteOnExit();
            Files.copy(inputItem.getInputStream(), Paths.get(tempFile.getPath()), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }

    private void releaseFile(Resource inputItem, File inputFile) {
        if (!inputItem.isFile()) {
            inputFile.delete();
        }
    }
}
