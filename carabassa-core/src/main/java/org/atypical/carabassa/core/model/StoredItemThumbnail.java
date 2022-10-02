package org.atypical.carabassa.core.model;

public interface StoredItemThumbnail {

	public static String THUMBNAIL_FORMAT = "jpg";

	public String getFilename();
	
	public void setFilename(String filename);

	public default String getFormat() {
		return THUMBNAIL_FORMAT;
	}

	public byte[] getContent();

	public void setContent(byte[] content);

}
