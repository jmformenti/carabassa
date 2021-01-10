package org.atypical.carabassa.cli.dto;

import java.nio.file.Path;

public class ItemToUpload {

	private String filename;
	private String contentType;
	private Path path;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}
}
