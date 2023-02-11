package org.atypical.carabassa.indexer.rdbms.component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.atypical.carabassa.core.component.indexer.DatasetIndexer;
import org.atypical.carabassa.core.component.tagger.Tagger;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.indexer.rdbms.entity.DatasetEntity;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedItemEntity;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.indexer.rdbms.entity.specification.ItemSpecification;
import org.atypical.carabassa.indexer.rdbms.repository.DatasetRepository;
import org.atypical.carabassa.indexer.rdbms.repository.IndexedItemRepository;
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
	private static final String ITEM_NOT_NULL_MESSAGE_KEY = "db.indexer.dataset.item.not_null";
	private static final String ITEM_ID_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.item.id_not_found";
	private static final String ITEM_HASH_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.item.hash_not_found";
	private static final String ITEM_EXISTS_MESSAGE_KEY = "db.indexer.dataset.item.exists";
	private static final String ITEM_BLANK_FILENAME_MESSAGE_KEY = "db.indexer.dataset.item.blank_filename";
	private static final String ITEM_CONTENT_NULL_MESSAGE_KEY = "db.indexer.dataset.item.content_null";
	private static final String ITEM_HASH_NULL_MESSAGE_KEY = "db.indexer.dataset.item.hash_null";
	private static final String ITEM_NOT_FILE_TYPE_MESSAGE_KEY = "db.indexer.dataset.item.not_file_type";
	private static final String ITEM_TYPE_NULL_MESSAGE_KEY = "db.indexer.dataset.item.type.not_null";
	private static final String METADATA_TAGGER_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.metadata_tagger.not_found";
	private static final String TAG_ID_NOT_FOUND_MESSAGE_KEY = "db.indexer.dataset.tag.id_not_found";
	private static final String TAG_NULL_MESSAGE_KEY = "db.indexer.dataset.tag.null";
	private static final String TAG_NAME_NULL_MESSAGE_KEY = "db.indexer.dataset.tag.name_null";

	@Autowired
	private DatasetRepository datasetRepository;

	@Autowired
	private IndexedItemRepository indexedItemRepository;

	@Autowired
	private LocalizedMessage localizedMessage;

	@Autowired
	private Map<String, Tagger> metadataTaggerByType;

	@PersistenceContext
	private EntityManager em;

	private IndexedItem addItem(Dataset dataset, IndexedItem indexedItem) {
		// Saves item in an efficient way to avoid problems with large sets performance
		// in hibernate
		indexedItem.setDataset(dataset);
		indexedItem = indexedItemRepository.save((IndexedItemEntity) indexedItem);
		// detach dataset from session to force reload next time is readed
		em.detach(dataset);
		return indexedItem;
	}

	@Override
	public IndexedItem addItem(Dataset dataset, ItemType type, String originalFilename, Resource inputItem)
			throws IOException, EntityExistsException {
		Assert.notNull(inputItem, localizedMessage.getText(ITEM_NOT_NULL_MESSAGE_KEY));
		IndexedItem indexedItem = build(type, originalFilename, inputItem);
		if (indexedItemRepository.findItemByHash(dataset, indexedItem.getHash()).isPresent()) {
			throw new EntityExistsException(localizedMessage.getText(ITEM_EXISTS_MESSAGE_KEY, indexedItem.getHash()));
		} else {
			return addItem(dataset, indexedItem);
		}
	}

	@Override
	public Long addItemTag(Dataset dataset, Long itemId, Tag tag) throws EntityNotFoundException {
		Assert.notNull(tag, localizedMessage.getText(TAG_NULL_MESSAGE_KEY));
		Assert.notNull(tag.getName(), localizedMessage.getText(TAG_NAME_NULL_MESSAGE_KEY));

		IndexedItem item = findItemById(dataset, itemId);
		item.getTags().add(tag);
		update(dataset);

		// recover persisted tag to return id
		item = findItemById(dataset, itemId);
		Tag persistedTag = item.getTags().stream().filter(t -> tag.equals(t)).findFirst().get();
		return persistedTag.getId();
	}

	protected IndexedItemEntity build(ItemType type, String originalFilename, Resource inputItem) throws IOException {
		Assert.isTrue(StringUtils.isNotBlank(originalFilename),
				localizedMessage.getText(ITEM_BLANK_FILENAME_MESSAGE_KEY));
		Assert.notNull(inputItem.getInputStream(), localizedMessage.getText(ITEM_CONTENT_NULL_MESSAGE_KEY));
		Assert.notNull(type, localizedMessage.getText(ITEM_TYPE_NULL_MESSAGE_KEY));

		IndexedItemEntity indexedItemEntity = new IndexedItemEntity();

		indexedItemEntity.setType(type);

		indexedItemEntity.setFilename(originalFilename);

		setMetaInfo(indexedItemEntity, inputItem);
		
		return indexedItemEntity;
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
	public void delete(Dataset dataset) {
		datasetRepository.delete((DatasetEntity) dataset);
	}

	@Override
	public void deleteAll() {
		datasetRepository.deleteAll();
	}

	@Override
	public void deleteItem(IndexedItem item) throws EntityNotFoundException {
		// Deletes item in an efficient way to avoid problems with large sets
		// performance in hibernate
		indexedItemRepository.delete((IndexedItemEntity) item);
		update(item.getDataset());
	}

	@Override
	public void deleteItemTag(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException {
		IndexedItem item = findItemById(dataset, itemId);
		Tag tag = findItemTagById(dataset, itemId, tagId);
		item.getTags().remove(tag);
		update(dataset);
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
	public Dataset findByName(String datasetName) throws EntityNotFoundException {
		return datasetRepository.findByName(datasetName).orElseThrow(() -> new EntityNotFoundException(
				localizedMessage.getText(DATASET_NAME_NOT_FOUND_MESSAGE_KEY, datasetName)));
	}

	@Override
	public IndexedItem findItemByHash(Dataset dataset, String hash) throws EntityNotFoundException {
		return indexedItemRepository.findItemByHash(dataset, hash).orElseThrow(
				() -> new EntityNotFoundException(localizedMessage.getText(ITEM_HASH_NOT_FOUND_MESSAGE_KEY, hash)));
	}

	@Override
	public IndexedItem findItemById(Dataset dataset, Long itemId) throws EntityNotFoundException {
		return indexedItemRepository.findItemById(dataset, itemId).orElseThrow(
				() -> new EntityNotFoundException(localizedMessage.getText(ITEM_ID_NOT_FOUND_MESSAGE_KEY, itemId)));
	}

	@Override
	public Page<IndexedItem> findItems(Dataset dataset, Pageable pageable) {
		return indexedItemRepository.findItems(dataset, pageable).map(item -> (IndexedItem) item);
	}

	@Override
	public Page<IndexedItem> findItems(Dataset dataset, SearchCriteria searchCriteria, Pageable pageable) {
		Assert.notNull(searchCriteria, "Search criteria can not be null.");
		return indexedItemRepository.findAll(new ItemSpecification(dataset, searchCriteria), pageable)
				.map(item -> (IndexedItem) item);
	}

	@Override
	public Tag findItemTagById(Dataset dataset, Long itemId, Long tagId) throws EntityNotFoundException {
		return findItemById(dataset, itemId).getTags() //
				.stream().filter(t -> tagId.equals(t.getId())) //
				.findFirst().orElseThrow(() -> new EntityNotFoundException(
						localizedMessage.getText(TAG_ID_NOT_FOUND_MESSAGE_KEY, tagId)));
	}

	@Override
	public IndexedItem resetItem(Dataset dataset, Long itemId, Resource inputItem) throws EntityNotFoundException, IOException {
		IndexedItem item = findItemById(dataset, itemId);
		item.getTags().clear();
		setMetaInfo(item, inputItem);
		item.setModification(Instant.now());
		update(dataset);
		return item;
	}

	@Override
	public Dataset update(Dataset dataset) {
		return datasetRepository.save((DatasetEntity) dataset);
	}

	private void setMetaInfo(IndexedItem indexedItem, Resource inputItem) throws IOException {
		Tagger metadataTagger = metadataTaggerByType.get(indexedItem.getType().normalized() + "MetadataTagger");
		Assert.notNull(metadataTagger, localizedMessage.getText(METADATA_TAGGER_NOT_FOUND_MESSAGE_KEY, indexedItem.getType()));

		Set<Tag> tags = metadataTagger.getTags(inputItem).stream().map(t -> new TagEntity(t))
				.collect(Collectors.toSet());
		if(indexedItem.getTags() == null) {
			indexedItem.setTags(tags);
		} else {
			indexedItem.getTags().addAll(tags);
		}

		String hash = indexedItem.getFirstTag(Tagger.TAG_HASH).getValue(String.class);
		Assert.notNull(hash, localizedMessage.getText(ITEM_HASH_NULL_MESSAGE_KEY));
		indexedItem.setHash(hash);

		Tag archiveTimeTag = indexedItem.getFirstTag(Tagger.TAG_ARCHIVE_TIME);
		Instant archiveTime = null;
		if (archiveTimeTag != null) {
			archiveTime = archiveTimeTag.getValue(Instant.class);
		}
		indexedItem.setArchiveTime(archiveTime);

		Tag fileTypeTag = indexedItem.getFirstTag(Tagger.TAG_FILE_TYPE);
		String fileType = fileTypeTag.getValue(String.class);
		Assert.isTrue(fileTypeTag != null && fileType != null,
				localizedMessage.getText(ITEM_NOT_FILE_TYPE_MESSAGE_KEY));
		indexedItem.setFormat(fileType);
		
		indexedItem.setSize(inputItem.contentLength());
	}

}
