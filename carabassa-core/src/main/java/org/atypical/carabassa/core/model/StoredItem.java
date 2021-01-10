package org.atypical.carabassa.core.model;

public interface StoredItem {

	public StoredItemInfo getStoredItemInfo();

	public void setStoredItemInfo(StoredItemInfo storedItemInfo);

	public byte[] getContent();

	public void setContent(byte[] content);

}
