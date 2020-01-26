package org.atypical.core.model;

public interface StoredImage {

	public StoredImageInfo getStoredImageInfo();

	public void setStoredImageInfo(StoredImageInfo storedImageInfo);

	public byte[] getContent();

	public void setContent(byte[] content);

}
