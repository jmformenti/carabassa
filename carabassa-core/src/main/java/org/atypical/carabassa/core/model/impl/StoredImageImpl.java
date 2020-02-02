package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.model.StoredImageInfo;

public class StoredImageImpl implements StoredImage {

	private StoredImageInfo storedImageInfo;
	private byte[] content;

	@Override
	public StoredImageInfo getStoredImageInfo() {
		return storedImageInfo;
	}

	@Override
	public void setStoredImageInfo(StoredImageInfo storedImageInfo) {
		this.storedImageInfo = storedImageInfo;
	}

	@Override
	public byte[] getContent() {
		return content;
	}

	@Override
	public void setContent(byte[] content) {
		this.content = content;
	}
}
