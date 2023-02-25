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

    void create(Dataset dataset) throws IOException, EntityExistsException;

    void addItem(IndexedItem item, Resource inputItem) throws IOException, EntityExistsException;

    StoredItem getItem(IndexedItem item) throws IOException, EntityNotFoundException;

    StoredItemThumbnail getItemThumbnail(IndexedItem item) throws IOException, EntityNotFoundException;

    void deleteAll() throws IOException;

    void delete(Dataset dataset) throws IOException;

    void deleteItem(IndexedItem item) throws IOException;

    void resetItem(IndexedItem updatedItem, IndexedItem previousItem) throws IOException, EntityExistsException;

    void update(String originalDatasetName, Dataset updatedDataset) throws IOException;

}
