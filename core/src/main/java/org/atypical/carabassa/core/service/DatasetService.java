package org.atypical.carabassa.core.service;

import java.io.IOException;
import java.util.List;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.StoredItemThumbnail;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatasetService {

    IndexedItem addItem(Dataset dataset, ItemType type, String originalFilename, Resource inputItem)
            throws IOException, EntityExistsException;

    Long addItemTag(Dataset dataset, Long itemId, Tag tag) throws EntityNotFoundException;

    Dataset create(Dataset dataset) throws IOException, EntityExistsException;

    void delete(Dataset dataset) throws IOException;

    void deleteAll() throws IOException;

    void deleteItem(Dataset dataset, Long itemId) throws IOException;

    void deleteItemTag(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException;

    List<Dataset> findAll();

    Page<Dataset> findAll(Pageable pageable);

    Dataset findById(Long datasetId) throws EntityNotFoundException;

    Page<IndexedItem> findItems(Dataset dataset, Pageable pageable);

    Page<IndexedItem> findItems(Dataset dataset, SearchCriteria searchCriteria, Pageable pageable);

    Dataset findByName(String datasetName) throws EntityNotFoundException;

    IndexedItem findItemById(Dataset dataset, Long itemId) throws EntityNotFoundException;

    IndexedItem findItemByHash(Dataset dataset, String hash) throws EntityNotFoundException;

    Tag findItemTagById(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException;

    StoredItem getStoredItem(Dataset dataset, IndexedItem item) throws IOException, EntityNotFoundException;

    StoredItemThumbnail getStoredItemThumbnail(Dataset dataset, IndexedItem item)
            throws IOException, EntityNotFoundException;

    void resetItem(Dataset dataset, Long itemId) throws EntityExistsException, EntityNotFoundException, IOException;

    Dataset update(String originalDatasetName, Dataset dataset) throws IOException;

}
