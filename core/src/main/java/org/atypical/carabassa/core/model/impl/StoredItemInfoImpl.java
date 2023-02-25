package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.StoredItemInfo;

public class StoredItemInfoImpl implements StoredItemInfo {

	private String originalFilename;

	public StoredItemInfoImpl() {
		super();
	}

	public StoredItemInfoImpl(String originalFilename) {
		super();
		this.originalFilename = originalFilename;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

}
