package org.atypical.carabassa.core.service.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.atypical.carabassa.core.component.indexer.DatasetIndexer;
import org.atypical.carabassa.core.component.storage.DatasetStorage;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.StoredItemThumbnail;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DatasetServiceImpl implements org.atypical.carabassa.core.service.DatasetService {

	private static final String DATASET_NAME_NOT_VALID_MESSAGE_KEY = "core.dataset.name_not_valid";

	private static final String REGEX_DATASET_NAME = "[a-zA-Z0-9_-]+";

	@Autowired
	private DatasetIndexer datasetIndexer;

	@Autowired
	private DatasetStorage datasetStorage;

	@Autowired
	private LocalizedMessage localizedMessage;

	@Override
	public IndexedItem addItem(Dataset dataset, ItemType type, String originalFilename, Resource inputItem)
			throws IOException, EntityExistsException {
		IndexedItem item = datasetIndexer.addItem(dataset, type, originalFilename, inputItem);
		datasetStorage.addItem(dataset, item, inputItem);
		return item;
	}

	@Override
	public Long addItemTag(Dataset dataset, Long itemId, Tag tag) throws EntityNotFoundException {
		return datasetIndexer.addItemTag(dataset, itemId, tag);
	}

	@Override
	public Dataset create(Dataset dataset) throws IOException, EntityExistsException {
		checkDatasetName(dataset.getName());
		datasetStorage.create(dataset);
		return datasetIndexer.create(dataset);
	}

	@Override
	public void delete(Dataset dataset) throws IOException {
		datasetStorage.delete(dataset);
		datasetIndexer.delete(dataset);
	}

	@Override
	public void deleteAll() throws IOException {
		datasetStorage.deleteAll();
		datasetIndexer.deleteAll();
	}

	@Override
	public void deleteItem(Dataset dataset, Long itemId) throws IOException {
		try {
			IndexedItem item = datasetIndexer.findItemById(dataset, itemId);
			datasetIndexer.deleteItem(dataset, item);
			datasetStorage.deleteItem(dataset, item);
		} catch (EntityNotFoundException e) {
			// nothing to do
		}
	}

	@Override
	public void deleteItemTag(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException {
		datasetIndexer.deleteItemTag(dataset, itemId, tagId);
	}

	@Override
	public List<Dataset> findAll() {
		return datasetIndexer.findAll();
	}

	@Override
	public Page<Dataset> findAll(Pageable pageable) {
		return datasetIndexer.findAll(pageable);
	}

	@Override
	public Dataset findById(Long datasetId) throws EntityNotFoundException {
		return datasetIndexer.findById(datasetId);
	}

	@Override
	public Page<IndexedItem> findItems(Dataset dataset, Pageable pageable) {
		return datasetIndexer.findItems(dataset, pageable);
	}

	@Override
	public Page<IndexedItem> findItems(Dataset dataset, SearchCriteria searchCriteria, Pageable pageable) {
		return datasetIndexer.findItems(dataset, searchCriteria, pageable);
	}

	@Override
	public Dataset findByName(String datasetName) throws EntityNotFoundException {
		return datasetIndexer.findByName(datasetName);
	}

	@Override
	public IndexedItem findItemById(Dataset dataset, Long itemId) throws EntityNotFoundException {
		return datasetIndexer.findItemById(dataset, itemId);
	}

	@Override
	public IndexedItem findItemByHash(Dataset dataset, String hash) throws EntityNotFoundException {
		return datasetIndexer.findItemByHash(dataset, hash);
	}

	@Override
	public Tag findItemTagById(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException {
		return datasetIndexer.findItemTagById(dataset, itemId, tagId);
	}

	@Override
	public StoredItem getStoredItem(Dataset dataset, IndexedItem item) throws IOException, EntityNotFoundException {
		return datasetStorage.getItem(dataset, item);
	}

	@Override
	public StoredItemThumbnail getStoredItemThumbnail(Dataset dataset, IndexedItem item) throws IOException, EntityNotFoundException {
		return datasetStorage.getItemThumbnail(dataset, item);
	}

	@Override
	public Dataset update(Dataset dataset) throws IOException {
		Dataset persistedDataset = datasetIndexer.update(dataset);
		datasetStorage.update(persistedDataset);
		return persistedDataset;
	}

	private void checkDatasetName(String name) {
		if (StringUtils.isBlank(name) || !name.matches(REGEX_DATASET_NAME)) {
			throw new IllegalArgumentException(localizedMessage.getText(DATASET_NAME_NOT_VALID_MESSAGE_KEY));
		}
	}

}
