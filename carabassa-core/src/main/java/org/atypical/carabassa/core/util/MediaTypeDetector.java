package org.atypical.carabassa.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.atypical.carabassa.core.model.enums.ItemType;

public class MediaTypeDetector {

	public static String detect(Path path) throws IOException {
		return Files.probeContentType(path);
	}

	public static ItemType convert(String contentType) {
		if (contentType != null) {
			if (contentType.startsWith("image/")) {
				return ItemType.IMAGE;
			} else if (contentType.startsWith("video/")) {
				return ItemType.VIDEO;
			}
		}
		return null;
	}

	public static String getSubType(String contentType) {
		if (contentType != null) {
			String[] tokens = contentType.split("/");
			if (tokens.length == 2) {
				return tokens[1];
			}
		}
		return null;
	}
}
