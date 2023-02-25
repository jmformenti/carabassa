package org.atypical.carabassa.core.model;

import org.springframework.core.io.Resource;

public interface StoredItem {

    StoredItemInfo getStoredItemInfo();

    void setStoredItemInfo(StoredItemInfo storedItemInfo);

    Resource getResource();

    void setResource(Resource resource);

}
