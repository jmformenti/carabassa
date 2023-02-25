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

	public IndexedItem addItem(Dataset dataset, ItemType type, String originalFilename, Resource inputItem)
			throws IOException, EntityExistsException;

	public Long addItemTag(Dataset dataset, Long itemId, Tag tag) throws EntityNotFoundException;

	public Dataset create(Dataset dataset) throws IOException, EntityExistsException;

	public void delete(Dataset dataset) throws IOException;

	public void deleteAll() throws IOException;

	public void deleteItem(Dataset dataset, Long itemId) throws IOException;

	public void deleteItemTag(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException;

	public List<Dataset> findAll();

	public Page<Dataset> findAll(Pageable pageable);

	public Dataset findById(Long datasetId) throws EntityNotFoundException;

	public Page<IndexedItem> findItems(Dataset dataset, Pageable pageable);

	public Page<IndexedItem> findItems(Dataset dataset, SearchCriteria searchCriteria, Pageable pageable);

	public Dataset findByName(String datasetName) throws EntityNotFoundException;

	public IndexedItem findItemById(Dataset dataset, Long itemId) throws EntityNotFoundException;

	public IndexedItem findItemByHash(Dataset dataset, String hash) throws EntityNotFoundException;

	public Tag findItemTagById(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException;

	public StoredItem getStoredItem(Dataset dataset, IndexedItem item) throws IOException, EntityNotFoundException;

	public StoredItemThumbnail getStoredItemThumbnail(Dataset dataset, IndexedItem item)
			throws IOException, EntityNotFoundException;

	public void resetItem(Dataset dataset, Long itemId) throws EntityExistsException, EntityNotFoundException, IOException;

	public Dataset update(String originalDatasetName, Dataset dataset) throws EntityNotFoundException, IOException;

}
