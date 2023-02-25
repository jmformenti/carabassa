package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.StoredItemInfo;
import org.springframework.core.io.Resource;

public class StoredItemImpl implements StoredItem {

    private StoredItemInfo storedItemInfo;
    private Resource resource;

    @Override
    public StoredItemInfo getStoredItemInfo() {
        return storedItemInfo;
    }

    @Override
    public void setStoredItemInfo(StoredItemInfo storedItemInfo) {
        this.storedItemInfo = storedItemInfo;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

}
