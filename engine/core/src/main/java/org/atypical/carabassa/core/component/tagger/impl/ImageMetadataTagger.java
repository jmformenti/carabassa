package org.atypical.carabassa.core.component.tagger.impl;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash;
import org.apache.commons.codec.binary.Hex;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

@Component
public class ImageMetadataTagger extends GenericMetadataTagger {

    private static final Logger logger = LoggerFactory.getLogger(ImageMetadataTagger.class);

    public static final String TAG_DHASH = TAG_PREFIX + "Dhash";

    private static final String IMAGE_ERROR_PHASH_MESSAGE_KEY = "core.tagger.phash.error";

    private static final DifferenceHash differenceHash = new DifferenceHash(64, DifferenceHash.Precision.Double);

    @Value("${carabassa.default-tz}")
    private String defaultTimeZone;

    @Value("${carabassa.tempdir:#{null}}")
    private String tempDirLocation;

    @Autowired
    private LocalizedMessage localizedMessage;

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
            tags.addAll(getCustomImageTags(inputItem, metadata));
        }

        if (logger.isTraceEnabled()) {
            super.printTags(tags);
        }

        return tags;
    }

    private Set<Tag> getCustomImageTags(Resource inputItem, Metadata metadata) throws IOException {
        Set<Tag> tags = new HashSet<>();

        try {
            tags.add(new TagImpl(TAG_DHASH, getPerceptualHash(inputItem)));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
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
            } else {
                Date dateModified = directory.getDateModified(timeZone);
                if (dateModified != null) {
                    return dateModified.toInstant();
                }
            }
        }
        return null;
    }

    private String getPerceptualHash(Resource inputItem) throws IOException {
        BufferedImage image;
        try {
            image = ImageIO.read(inputItem.getInputStream());
        } catch (IOException e) {
            throw new IOException(localizedMessage.getText(IMAGE_ERROR_PHASH_MESSAGE_KEY, e.getMessage()), e);
        }
        if (image == null) {
            throw new IOException(localizedMessage.getText(IMAGE_ERROR_PHASH_MESSAGE_KEY, "null image"));
        }

        Hash hash = differenceHash.hash(image);

        return Hex.encodeHexString(hash.toByteArray());
    }
}
