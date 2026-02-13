package org.atypical.carabassa.core.component.indexer;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface DatasetIndexer {

    IndexedItem addItem(Dataset dataset, ItemType type, String originalFilename, Resource inputItem)
            throws IOException, EntityExistsException;

    Long addItemTag(Dataset dataset, Long itemId, Tag tag) throws EntityNotFoundException;

    Dataset create(Dataset dataset) throws EntityExistsException;

    void delete(Dataset dataset);

    void deleteAll();

    void deleteItem(IndexedItem item);

    void deleteItemTag(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException;

    List<Dataset> findAll();

    Page<Dataset> findAll(Pageable pageable);

    Dataset findById(Long datasetId) throws EntityNotFoundException;

    Dataset findByName(String datasetName) throws EntityNotFoundException;

    IndexedItem findItemByHash(Dataset dataset, String hash) throws EntityNotFoundException;

    IndexedItem findItemById(Dataset dataset, Long itemId) throws EntityNotFoundException;

    Page<IndexedItem> findItems(Dataset dataset, Pageable pageable);

    Page<IndexedItem> findItems(Dataset dataset, SearchCriteria searchCriteria, Pageable pageable);

    Tag findItemTagById(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException;

    IndexedItem reindex(Dataset dataset, Long itemId, Resource inputItem) throws EntityNotFoundException, IOException;

    Dataset update(Dataset dataset);

}
