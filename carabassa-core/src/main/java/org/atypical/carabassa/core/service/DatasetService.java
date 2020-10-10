package org.atypical.carabassa.core.service;

import java.io.IOException;
import java.util.List;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.model.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatasetService {

	public IndexedImage addImage(Dataset dataset, Resource inputImage) throws IOException, EntityExistsException;

	public Long addImageTag(Dataset dataset, Long imageId, Tag tag) throws EntityNotFoundException;

	public Dataset create(Dataset dataset) throws IOException, EntityExistsException;

	public void delete(Dataset dataset) throws IOException;

	public void deleteAll() throws IOException;

	public void deleteImage(Dataset dataset, Long imageId) throws IOException;

	public void deleteImageTag(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException;

	public List<Dataset> findAll();

	public Page<Dataset> findAll(Pageable pageable);

	public Dataset findById(Long datasetId) throws EntityNotFoundException;

	public Page<IndexedImage> findImages(Dataset dataset, Pageable pageable);

	public Dataset findByName(String datasetName) throws EntityNotFoundException;

	public IndexedImage findImageById(Dataset dataset, Long imageId) throws EntityNotFoundException;

	public IndexedImage findImageByHash(Dataset dataset, String hash) throws EntityNotFoundException;

	public Tag findImageTagById(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException;

	public StoredImage getStoredImage(Dataset dataset, IndexedImage image) throws IOException, EntityNotFoundException;

	public Dataset update(Dataset dataset) throws IOException;

}
