package org.atypical.carabassa.core.model;

public interface StoredItemThumbnail {

    String THUMBNAIL_FORMAT = "jpg";

    String getFilename();

    void setFilename(String filename);

    default String getFormat() {
        return THUMBNAIL_FORMAT;
    }

    byte[] getContent();

    void setContent(byte[] content);

}
