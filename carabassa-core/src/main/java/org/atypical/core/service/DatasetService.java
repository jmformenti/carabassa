package org.atypical.core.service;

import java.io.IOException;
import java.util.List;

import org.atypical.core.exception.EntityExistsException;
import org.atypical.core.exception.EntityNotFoundException;
import org.atypical.core.model.Dataset;
import org.atypical.core.model.IndexedImage;
import org.atypical.core.model.StoredImage;
import org.atypical.core.model.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatasetService {

	public IndexedImage addImage(Dataset dataset, Resource inputImage) throws IOException, EntityExistsException;

	public Long addImageTag(Dataset dataset, Long imageId, Tag tag) throws EntityNotFoundException;

	public Dataset create(String datasetName) throws IOException, EntityExistsException;

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

	public Tag findImageTagById(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException;

	public StoredImage getStoredImage(Dataset dataset, IndexedImage image) throws IOException, EntityNotFoundException;

	public Dataset update(Dataset dataset) throws IOException;

}
