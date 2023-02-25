package org.atypical.carabassa.core.component.storage;

import java.io.IOException;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.StoredItemThumbnail;
import org.springframework.core.io.Resource;

public interface DatasetStorage {

	public void create(Dataset dataset) throws IOException, EntityExistsException;

	public void addItem(IndexedItem item, Resource inputItem) throws IOException, EntityExistsException;

	public StoredItem getItem(IndexedItem item) throws IOException, EntityNotFoundException;

	public StoredItemThumbnail getItemThumbnail(IndexedItem item) throws IOException, EntityNotFoundException;

	public void deleteAll() throws IOException;

	public void delete(Dataset dataset) throws IOException;

	public void deleteItem(IndexedItem item) throws IOException;

	public void resetItem(IndexedItem updatedItem, IndexedItem previousItem) throws IOException, EntityExistsException;

	public void update(String originalDatasetName, Dataset updatedDataset) throws IOException;

}
