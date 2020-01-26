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
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.model.Tag;
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
	public IndexedImage addImage(Dataset dataset, Resource inputImage) throws IOException, EntityExistsException {
		IndexedImage image = datasetIndexer.addImage(dataset, inputImage);
		datasetStorage.addImage(dataset, image, inputImage);
		return image;
	}

	@Override
	public Long addImageTag(Dataset dataset, Long imageId, Tag tag) throws EntityNotFoundException {
		return datasetIndexer.addImageTag(dataset, imageId, tag);
	}

	@Override
	public Dataset create(String datasetName) throws IOException, EntityExistsException {
		checkDatasetName(datasetName);
		datasetStorage.create(datasetName);
		return datasetIndexer.create(datasetName);
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
	public void deleteImage(Dataset dataset, Long imageId) throws IOException {
		try {
			IndexedImage indexedImage = datasetIndexer.findImageById(dataset, imageId);
			datasetIndexer.deleteImage(dataset, indexedImage);
			datasetStorage.deleteImage(dataset, indexedImage);
		} catch (EntityNotFoundException e) {
			// nothing to do
		}
	}

	@Override
	public void deleteImageTag(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException {
		datasetIndexer.deleteImageTag(dataset, imageId, tagId);
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
	public Page<IndexedImage> findImages(Dataset dataset, Pageable pageable) {
		return datasetIndexer.findImages(dataset, pageable);
	}

	@Override
	public Dataset findByName(String datasetName) throws EntityNotFoundException {
		return datasetIndexer.findByName(datasetName);
	}

	@Override
	public IndexedImage findImageById(Dataset dataset, Long imageId) throws EntityNotFoundException {
		return datasetIndexer.findImageById(dataset, imageId);
	}

	@Override
	public Tag findImageTagById(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException {
		return datasetIndexer.findImageTagById(dataset, imageId, tagId);
	}

	@Override
	public StoredImage getStoredImage(Dataset dataset, IndexedImage indexedImage)
			throws IOException, EntityNotFoundException {
		return datasetStorage.getImage(dataset, indexedImage);
	}

	@Override
	public Dataset update(Dataset dataset) throws IOException {
		datasetStorage.update(dataset);
		return datasetIndexer.update(dataset);
	}

	private void checkDatasetName(String name) {
		if (StringUtils.isBlank(name) || !name.matches(REGEX_DATASET_NAME)) {
			throw new IllegalArgumentException(localizedMessage.getText(DATASET_NAME_NOT_VALID_MESSAGE_KEY));
		}
	}

}
