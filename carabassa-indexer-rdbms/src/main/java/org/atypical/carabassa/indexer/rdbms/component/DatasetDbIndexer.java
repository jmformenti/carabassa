package org.atypical.carabassa.indexer.rdbms.component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.atypical.carabassa.core.component.indexer.DatasetIndexer;
import org.atypical.carabassa.core.component.tagger.impl.ImageMetadataTagger;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.indexer.rdbms.entity.DatasetEntity;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedImageEntity;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.indexer.rdbms.repository.DatasetRepository;
import org.atypical.carabassa.indexer.rdbms.repository.IndexedImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Transactional(rollbackOn = Exception.class)
public class DatasetDbIndexer implements DatasetIndexer {

	private static final String DATASET_ID_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.id_not_found";
	private static final String DATASET_NAME_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.name_not_found";
	private static final String DATASET_EXISTS_MESSAGE_KEY = "db.indexer.dataset.exists";
	private static final String IMAGE_NOT_NULL_MESSAGE_KEY = "db.indexer.dataset.image.not_null";
	private static final String IMAGE_ID_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.image.id_not_found";
	private static final String IMAGE_EXISTS_MESSAGE_KEY = "db.indexer.dataset.image.exists";
	private static final String IMAGE_BLANK_FILENAME_MESSAGE_KEY = "db.indexer.dataset.image.blank_filename";
	private static final String IMAGE_CONTENT_NULL_MESSAGE_KEY = "db.indexer.dataset.image.content_null";
	private static final String IMAGE_HASH_NULL_MESSAGE_KEY = "db.indexer.dataset.image.hash_null";
	private static final String IMAGE_NOT_FILE_TYPE_MESSAGE_KEY = "db.indexer.dataset.image.not_file_type";
	private static final String TAG_ID_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.tag.id_not_found";
	private static final String TAG_NULL_MESSAGE_KEY = "db.indexer.dataset.tag.null";
	private static final String TAG_NAME_NULL_MESSAGE_KEY = "db.indexer.dataset.tag.name_null";

	@Autowired
	private ImageMetadataTagger imageMetadataTagger;

	@Autowired
	private DatasetRepository datasetRepository;

	@Autowired
	private IndexedImageRepository indexedImageRepository;

	@Autowired
	private LocalizedMessage localizedMessage;

	@PersistenceContext
	private EntityManager em;

	@Override
	public IndexedImage addImage(Dataset dataset, Resource inputImage) throws IOException, EntityExistsException {
		Assert.notNull(inputImage, localizedMessage.getText(IMAGE_NOT_NULL_MESSAGE_KEY));
		IndexedImage image = build(inputImage);
		if (indexedImageRepository.findImageByHash(dataset, image.getHash()).isPresent()) {
			throw new EntityExistsException(localizedMessage.getText(IMAGE_EXISTS_MESSAGE_KEY, image.getHash()));
		} else {
			return addImage(dataset, image);
		}
	}

	private IndexedImage addImage(Dataset dataset, IndexedImage indexedImage) {
		// Saves image in an efficient way to avoid problems with large sets performance
		// in hibernate
		indexedImage.setDataset(dataset);
		indexedImage = indexedImageRepository.save((IndexedImageEntity) indexedImage);
		// detach dataset from session to force reload next time is readed
		em.detach(dataset);
		return indexedImage;
	}

	@Override
	public Long addImageTag(Dataset dataset, Long imageId, Tag tag) throws EntityNotFoundException {
		Assert.notNull(tag, localizedMessage.getText(TAG_NULL_MESSAGE_KEY));
		Assert.notNull(tag.getName(), localizedMessage.getText(TAG_NAME_NULL_MESSAGE_KEY));

		IndexedImage indexedImage = findImageById(dataset, imageId);
		indexedImage.getTags().add(tag);
		update(dataset);

		// recover persisted tag to return id
		indexedImage = findImageById(dataset, imageId);
		Tag persistedTag = indexedImage.getTags().stream().filter(t -> tag.equals(t)).findFirst().get();
		return persistedTag.getId();
	}

	private IndexedImageEntity build(Resource inputImage) throws IOException {
		Assert.isTrue(StringUtils.isNotBlank(inputImage.getFilename()),
				localizedMessage.getText(IMAGE_BLANK_FILENAME_MESSAGE_KEY));
		Assert.notNull(inputImage.getInputStream(), localizedMessage.getText(IMAGE_CONTENT_NULL_MESSAGE_KEY));

		IndexedImageEntity image = new IndexedImageEntity();

		image.setFilename(inputImage.getFilename());

		Set<Tag> tags = imageMetadataTagger.getTags(inputImage).stream().map(t -> new TagEntity(t))
				.collect(Collectors.toSet());
		image.setTags(tags);

		String hash = image.getFirstTag(ImageMetadataTagger.TAG_HASH).getValue(String.class);
		Assert.notNull(hash, localizedMessage.getText(IMAGE_HASH_NULL_MESSAGE_KEY));
		image.setHash(hash);

		Tag archiveTimeTag = image.getFirstTag(ImageMetadataTagger.TAG_ARCHIVE_TIME);
		ZonedDateTime archiveTime = null;
		if (archiveTimeTag != null) {
			archiveTime = archiveTimeTag.getValue(ZonedDateTime.class);
		}
		image.setArchiveTime(archiveTime);

		Tag fileTypeTag = image.getFirstTag(ImageMetadataTagger.TAG_FILE_TYPE);
		String fileType = fileTypeTag.getValue(String.class);
		Assert.isTrue(fileTypeTag != null && fileType != null,
				localizedMessage.getText(IMAGE_NOT_FILE_TYPE_MESSAGE_KEY));
		image.setFileType(fileType);

		return image;
	}

	@Override
	public Dataset create(Dataset dataset) throws EntityExistsException {
		String datasetName = dataset.getName();
		Optional<DatasetEntity> persistedDataset = datasetRepository.findByName(datasetName);
		if (persistedDataset.isPresent()) {
			throw new EntityExistsException(localizedMessage.getText(DATASET_EXISTS_MESSAGE_KEY, datasetName));
		} else {
			DatasetEntity datasetEntity = new DatasetEntity(datasetName);
			datasetEntity.setDescription(dataset.getDescription());
			return update(datasetEntity);
		}
	}

	@Override
	public List<Dataset> findAll() {
		return StreamSupport.stream(datasetRepository.findAll().spliterator(), false).collect(Collectors.toList());
	}

	@Override
	public Page<Dataset> findAll(Pageable pageable) {
		return datasetRepository.findAll(pageable).map(d -> (DatasetEntity) d);
	}

	@Override
	public Dataset findById(Long datasetId) throws EntityNotFoundException {
		return datasetRepository.findById(datasetId).orElseThrow(() -> new EntityNotFoundException(
				localizedMessage.getText(DATASET_ID_NOT_FOUND_MESSAGE_KEY, datasetId)));

	}

	@Override
	public Page<IndexedImage> findImages(Dataset dataset, Pageable pageable) {
		return indexedImageRepository.findImages(dataset, pageable);
	}

	@Override
	public Dataset findByName(String datasetName) throws EntityNotFoundException {
		return datasetRepository.findByName(datasetName).orElseThrow(() -> new EntityNotFoundException(
				localizedMessage.getText(DATASET_NAME_NOT_FOUND_MESSAGE_KEY, datasetName)));
	}

	@Override
	public IndexedImage findImageById(Dataset dataset, Long imageId) throws EntityNotFoundException {
		return indexedImageRepository.findImageById(dataset, imageId).orElseThrow(
				() -> new EntityNotFoundException(localizedMessage.getText(IMAGE_ID_NOT_FOUND_MESSAGE_KEY, imageId)));
	}

	@Override
	public Tag findImageTagById(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException {
		return findImageById(dataset, imageId).getTags() //
				.stream().filter(t -> tagId.equals(t.getId())) //
				.findFirst().orElseThrow(() -> new EntityNotFoundException(
						localizedMessage.getText(TAG_ID_NOT_FOUND_MESSAGE_KEY, tagId)));
	}

	@Override
	public void deleteAll() {
		datasetRepository.deleteAll();
	}

	@Override
	public void delete(Dataset dataset) {
		datasetRepository.delete((DatasetEntity) dataset);
	}

	@Override
	public void deleteImage(Dataset dataset, IndexedImage indexedImage) throws EntityNotFoundException {
		// detach dataset from session before get an inconsistent status
		em.detach(dataset);
		// Deletes image in an efficient way to avoid problems with large sets
		// performance in hibernate
		indexedImageRepository.delete((IndexedImageEntity) indexedImage);
	}

	@Override
	public void deleteImageTag(Dataset dataset, Long imageId, Long tagId) throws EntityNotFoundException {
		IndexedImage indexedImage = findImageById(dataset, imageId);
		Tag tag = findImageTagById(dataset, imageId, tagId);
		indexedImage.getTags().remove(tag);
		update(dataset);
	}

	@Override
	public Dataset update(Dataset dataset) {
		return datasetRepository.save((DatasetEntity) dataset);
	}

}
