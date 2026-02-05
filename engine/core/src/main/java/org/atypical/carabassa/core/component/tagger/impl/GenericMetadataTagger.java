package org.atypical.carabassa.core.component.tagger.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.text.WordUtils;
import org.atypical.carabassa.core.component.tagger.Tagger;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.atypical.carabassa.core.util.HashGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import atlas.Atlas;
import atlas.City;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.file.FileTypeDirectory;

public abstract class GenericMetadataTagger implements Tagger {

    private static final Logger logger = LoggerFactory.getLogger(GenericMetadataTagger.class);

    public static final String TAG_GEO_LATITUDE = TAG_PREFIX + "GeoLatitude";
    public static final String TAG_GEO_LONGITUDE = TAG_PREFIX + "GeoLongitude";
    public static final String TAG_CITY = TAG_PREFIX + "City";

    private static final String IMAGE_ERROR_META_MESSAGE_KEY = "core.tagger.meta.image.error";

    @Value("${carabassa.default-tz}")
    private String defaultTimeZone;

    @Value("${carabassa.tempdir:#{null}}")
    private String tempDirLocation;

    @Autowired
    private LocalizedMessage localizedMessage;

    private final Atlas atlas = new Atlas();

    protected Metadata getMetaData(Resource inputItem) throws IOException {
        Metadata metadata;
        try {
            metadata = ImageMetadataReader.readMetadata(inputItem.getInputStream());
        } catch (ImageProcessingException | IOException e) {
            throw new IOException(localizedMessage.getText(IMAGE_ERROR_META_MESSAGE_KEY, e.getMessage()), e);
        }
        if (logger.isTraceEnabled()) {
            printMetadata(metadata);
        }
        return metadata;
    }

    protected Set<Tag> getTags(Resource inputItem, Metadata metadata) throws IOException {
        Set<Tag> tags = new HashSet<>();

        if (metadata != null) {
            tags.addAll(getMetaTags(metadata));
            tags.addAll(getCustomTags(inputItem, metadata));
        }

        return tags;
    }

    protected void printTags(Set<Tag> tags) {
        tags.forEach(t -> logger.debug("Tag {} = {}", t.getName(), t.getValue()));
    }

    private Set<Tag> getCustomTags(Resource inputItem, Metadata metadata) throws IOException {
        Set<Tag> tags = new HashSet<>();

        tags.add(new TagImpl(TAG_HASH, HashGenerator.generate(inputItem)));
        tags.add(new TagImpl(TAG_FILE_TYPE, getFileType(metadata)));

        return tags;
    }

    private void printMetadata(Metadata metadata) {
        for (Directory directory : metadata.getDirectories()) {
            for (com.drew.metadata.Tag tag : directory.getTags()) {
                logger.debug(String.format("%s (%s) = str(%s), date(%s)", directory.getTagName(tag.getTagType()),
                        tag.getTagTypeHex(), directory.getString(tag.getTagType()),
                        directory.getDate(tag.getTagType())));
            }
        }
    }

    private Set<Tag> getMetaTags(Metadata metadata) {
        Set<Tag> metaTags = new HashSet<>();
        for (Directory directory : metadata.getDirectories()) {
            if (directory instanceof GpsDirectory) {
                addGeoTags((GpsDirectory) directory, metaTags);
            }
            for (com.drew.metadata.Tag metaTag : directory.getTags()) {
                Object tagValue = directory.getObject(metaTag.getTagType());
                if (isValidTag(metaTag.getTagName(), tagValue)) {
                    Tag tag = new TagImpl(TAG_PREFIX + toCamelCase(metaTag.getTagName()), tagValue);
                    metaTags.add(tag);
                }
            }
        }
        return metaTags;
    }

    private void addGeoTags(GpsDirectory directory, Set<Tag> metaTags) {
        GeoLocation geoLocation = directory.getGeoLocation();
        if (geoLocation != null && !geoLocation.isZero()) {
            metaTags.add(new TagImpl(TAG_GEO_LATITUDE, geoLocation.getLatitude()));
            metaTags.add(new TagImpl(TAG_GEO_LONGITUDE, geoLocation.getLongitude()));
            City city = atlas.find(geoLocation.getLatitude(), geoLocation.getLongitude());
            if (city != null) {
                metaTags.add(new TagImpl(TAG_CITY, city.name));
            }
        }
    }

    private boolean isValidTag(String tagName, Object value) {
        if (tagName == null) {
            return false;
        } else if (tagName.startsWith("Unknown tag")) {
            return false;
        } else if (value == null) {
            return false;
        } else if (value instanceof String) {
            return !((String) value).isEmpty();
        } else if (value instanceof byte[]) {
            byte[] data = (byte[]) value;
            return !IntStream.range(0, data.length).parallel().allMatch(i -> data[i] == 0);
        }
        return true;
    }

    private String toCamelCase(String name) {
        return WordUtils.capitalizeFully(name.replaceAll("/", ""), new char[] { '_', ' ' }).replaceAll("[ _]", "");
    }

    private String getFileType(Metadata metadata) {
        FileTypeDirectory directory = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
        if (directory != null) {
            String extension = directory.getString(FileTypeDirectory.TAG_EXPECTED_FILE_NAME_EXTENSION);
            if (extension != null) {
                return extension;
            } else {
                return directory.getString(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_NAME);
            }
        } else {
            return null;
        }
    }
}
