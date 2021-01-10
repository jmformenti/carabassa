package org.atypical.carabassa.core.component.storage;

import java.io.IOException;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.StoredItem;
import org.springframework.core.io.Resource;

public interface DatasetStorage {

	public void create(Dataset dataset) throws IOException, EntityExistsException;

	public void addItem(Dataset dataset, IndexedItem item, Resource inputItem)
			throws IOException, EntityExistsException;

	public StoredItem getItem(Dataset dataset, IndexedItem item) throws IOException, EntityNotFoundException;

	public void deleteAll() throws IOException;

	public void delete(Dataset dataset) throws IOException;

	public void deleteItem(Dataset dataset, IndexedItem item) throws IOException;

	public void update(Dataset dataset) throws IOException;

}
