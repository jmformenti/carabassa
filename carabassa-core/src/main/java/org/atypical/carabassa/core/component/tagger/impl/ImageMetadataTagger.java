package org.atypical.carabassa.core.component.tagger.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.WordUtils;
import org.atypical.carabassa.core.component.tagger.Tagger;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.atypical.carabassa.core.util.HashGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileTypeDirectory;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;

@Component
public class ImageMetadataTagger implements Tagger {

	private static final Logger logger = LoggerFactory.getLogger(ImageMetadataTagger.class);

	public static final String TAG_DHASH = TAG_PREFIX + "Dhash";

	private static final String IMAGE_ERROR_META_MESSAGE_KEY = "core.tagger.meta.image.error";
	private static final String IMAGE_ERROR_PHASH_MESSAGE_KEY = "core.tagger.phash.error";

	@Autowired
	private LocalizedMessage localizedMessage;

	@Override
	public Set<Tag> getTags(Resource inputItem) throws IOException {
		Set<Tag> tags = new HashSet<>();

		Metadata metadata = getMetaData(inputItem);
		if (metadata != null) {
			tags.addAll(getMetaTags(metadata));
			tags.addAll(getCustomTags(inputItem, metadata));
		}

		tags.forEach(t -> logger.debug("Tag {} = {}", t.getName(), t.getValue()));

		return tags;
	}

	private Set<Tag> getCustomTags(Resource inputItem, Metadata metadata) throws IOException {
		Set<Tag> tags = new HashSet<>();

		tags.add((Tag) new TagImpl(TAG_HASH, HashGenerator.generate(inputItem)));
		try {
			tags.add((Tag) new TagImpl(TAG_DHASH, getPerceptualHash(inputItem)));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		ZonedDateTime archiveTime = getArchiveTime(metadata);
		if (archiveTime != null) {
			tags.add((Tag) new TagImpl(TAG_ARCHIVE_TIME, archiveTime));
		}
		tags.add((Tag) new TagImpl(TAG_FILE_TYPE, getFileType(metadata)));

		return tags;
	}

	private Metadata getMetaData(Resource inputItem) throws IOException {
		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata(inputItem.getInputStream());
		} catch (ImageProcessingException | IOException e) {
			throw new IOException(localizedMessage.getText(IMAGE_ERROR_META_MESSAGE_KEY, e.getMessage()), e);
		}
		return metadata;
	}

	private Set<Tag> getMetaTags(Metadata metadata) {
		Set<Tag> metaTags = new HashSet<>();
		for (Directory directory : metadata.getDirectories()) {
			for (com.drew.metadata.Tag metaTag : directory.getTags()) {
				Object tagValue = directory.getObject(metaTag.getTagType());
				if (isValidValue(tagValue)) {
					Tag tag = new TagImpl(TAG_PREFIX + toCamelCase(metaTag.getTagName()), tagValue);
					metaTags.add((Tag) tag);
				}
			}
		}
		return metaTags;
	}

	private boolean isValidValue(Object value) {
		if (value == null) {
			return false;
		} else if (value instanceof String) {
			if (((String) value).isEmpty()) {
				return false;
			}
		} else if (value instanceof byte[]) {
			byte[] data = (byte[]) value;
			return !IntStream.range(0, data.length).parallel().allMatch(i -> data[i] == 0);
		}
		return true;
	}

	private String toCamelCase(String name) {
		return WordUtils.capitalizeFully(name.replaceAll("/", ""), new char[] { '_', ' ' }).replaceAll("[ _]", "");
	}

	private ZonedDateTime getArchiveTime(Metadata metadata) {
		ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
		if (directory != null && directory.getDateOriginal() != null) {
			return directory.getDateOriginal().toInstant().atZone(ZoneId.of("UTC"));
		} else {
			return null;
		}
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

	private String getPerceptualHash(Resource inputItem) throws IOException {
		DifferenceHash differenceHash = new DifferenceHash(64, Precision.Double);

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
