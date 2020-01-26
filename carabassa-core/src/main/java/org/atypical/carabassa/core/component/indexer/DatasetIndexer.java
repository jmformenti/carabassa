package org.atypical.carabassa.core.component.indexer;

import java.io.IOException;
import java.util.List;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatasetIndexer {

	public IndexedImage addImage(Dataset dataset, Resource inputImage) throws IOException, EntityExistsException;

	public Long addImageTag(Dataset dataset, Long imageId, Tag tag) throws EntityNotFoundException;

	public Dataset create(String datasetName) throws EntityExistsException;

	public List<Dataset> findAll();

	public Page<Dataset> findAll(Pageable pageable);

	public Dataset findById(Long datasetId) throws EntityNotFoundException;

	public Page<IndexedImage> findImages(Dataset dataset, Pageable pageable);

	public Dataset findByName(String datasetName) throws EntityNotFoundException;

	public IndexedImage findImageById(Dataset dataset, Long imageId) throws EntityNotFoundException;

	public Tag findImageTagById(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException;

	public void deleteAll();

	public void delete(Dataset dataset);

	public void deleteImage(Dataset dataset, IndexedImage imageId) throws EntityNotFoundException;

	public void deleteImageTag(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException;

	public Dataset update(Dataset dataset);

}
