package org.atypical.core.model.impl;

import org.atypical.core.model.StoredImage;
import org.atypical.core.model.StoredImageInfo;

public class StoredImageImpl implements StoredImage {

	private StoredImageInfo storedImageInfo;
	private byte[] content;

	public StoredImageInfo getStoredImageInfo() {
		return storedImageInfo;
	}

	public void setStoredImageInfo(StoredImageInfo storedImageInfo) {
		this.storedImageInfo = storedImageInfo;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}
}
