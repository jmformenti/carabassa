package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.StoredItemThumbnail;

public class StoredItemThumbnailImpl implements StoredItemThumbnail {

    private String filename;
    private byte[] content;

    public StoredItemThumbnailImpl(String filename, byte[] content) {
        super();
        this.filename = filename;
        this.content = content;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
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
