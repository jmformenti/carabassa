package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.StoredImageInfo;

public class StoredImageInfoImpl implements StoredImageInfo {

	private String originalFilename;

	public StoredImageInfoImpl() {
		super();
	}

	public StoredImageInfoImpl(String originalFilename) {
		super();
		this.originalFilename = originalFilename;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

}
