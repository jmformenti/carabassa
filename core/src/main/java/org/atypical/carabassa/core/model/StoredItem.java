package org.atypical.carabassa.core.model;

import org.springframework.core.io.Resource;

public interface StoredItem {

	public StoredItemInfo getStoredItemInfo();

	public void setStoredItemInfo(StoredItemInfo storedItemInfo);

	public Resource getResource();

	public void setResource(Resource resource);

}
