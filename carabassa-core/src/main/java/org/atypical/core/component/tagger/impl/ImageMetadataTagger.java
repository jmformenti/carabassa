package org.atypical.core.component.tagger.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.WordUtils;
import org.atypical.core.component.tagger.Tagger;
import org.atypical.core.component.util.LocalizedMessage;
import org.atypical.core.model.Tag;
import org.atypical.core.model.impl.TagImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

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

	private static final String IMAGE_ERROR_MESSAGE_KEY = "core.tagger.meta.image.error";

	public static final String TAG_PREFIX = "meta.";
	public static final String TAG_HASH = TAG_PREFIX + "Hash";
	public static final String TAG_DHASH = TAG_PREFIX + "Dhash";
	public static final String TAG_ARCHIVE_TIME = TAG_PREFIX + "ArchiveTime";
	public static final String TAG_FILE_TYPE = TAG_PREFIX + "FileType";

	@Autowired
	private LocalizedMessage localizedMessage;

	@Override
	public Set<Tag> getTags(Resource inputImage) throws IOException {
		Set<Tag> tags = new HashSet<>();

		byte[] content = IOUtils.toByteArray(inputImage.getInputStream());
		Metadata metadata = getMetaData(content);
		if (metadata != null) {
			tags.addAll(getMetaTags(metadata));
			tags.addAll(getCustomTags(content, metadata));
		}

		return tags;
	}

	private Set<Tag> getCustomTags(byte[] content, Metadata metadata) throws IOException {
		Set<Tag> tags = new HashSet<>();

		tags.add((Tag) new TagImpl(TAG_HASH, getHash(content)));
		tags.add((Tag) new TagImpl(TAG_DHASH, getPerceptualHash(content)));
		ZonedDateTime archiveTime = getArchiveTime(metadata);
		if (archiveTime != null) {
			tags.add((Tag) new TagImpl(TAG_ARCHIVE_TIME, archiveTime));
		}
		tags.add((Tag) new TagImpl(TAG_FILE_TYPE, getFileType(metadata)));

		return tags;
	}

	private String getHash(byte[] content) throws IOException {
		return DigestUtils.md5DigestAsHex(content);
	}

	private Metadata getMetaData(byte[] content) throws IOException {
		Metadata metadata = null;
		InputStream imageStream = new ByteArrayInputStream(content);
		try {
			metadata = ImageMetadataReader.readMetadata(imageStream);
		} catch (ImageProcessingException | IOException e) {
			throw new IOException(localizedMessage.getText(IMAGE_ERROR_MESSAGE_KEY), e);
		}
		return metadata;
	}

	private Set<Tag> getMetaTags(Metadata metadata) {
		Set<Tag> metaTags = new HashSet<>();
		for (Directory directory : metadata.getDirectories()) {
			for (com.drew.metadata.Tag metaTag : directory.getTags()) {
				Tag tag = new TagImpl(TAG_PREFIX + toCamelCase(metaTag.getTagName()),
						directory.getObject(metaTag.getTagType()));
				metaTags.add((Tag) tag);
			}
		}
		return metaTags;
	}

	private String toCamelCase(String name) {
		return WordUtils.capitalizeFully(name.replaceAll("/", ""), new char[] { '_', ' ' }).replaceAll("[ _]", "");
	}

	private ZonedDateTime getArchiveTime(Metadata metadata) {
		ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
		if (directory != null) {
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

	private String getPerceptualHash(byte[] content) throws IOException {
		DifferenceHash differenceHash = new DifferenceHash(64, Precision.Double);

		InputStream in = new ByteArrayInputStream(content);
		BufferedImage image = ImageIO.read(in);
		Hash hash = differenceHash.hash(image);

		return Hex.encodeHexString(hash.toByteArray());
	}

}
