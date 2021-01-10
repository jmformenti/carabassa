package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.StoredItemInfo;

public class StoredItemImpl implements StoredItem {

	private StoredItemInfo storedItemInfo;
	private byte[] content;

	@Override
	public StoredItemInfo getStoredItemInfo() {
		return storedItemInfo;
	}

	@Override
	public void setStoredItemInfo(StoredItemInfo storedItemInfo) {
		this.storedItemInfo = storedItemInfo;
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
