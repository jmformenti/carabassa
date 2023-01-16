package org.atypical.carabassa.indexer.rdbms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.atypical.carabassa.core.component.tagger.Tagger;
import org.atypical.carabassa.core.component.tagger.impl.ImageMetadataTagger;
import org.atypical.carabassa.core.configuration.CoreConfiguration;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.atypical.carabassa.core.model.impl.DatasetImpl;
import org.atypical.carabassa.core.model.impl.SearchConditionImpl;
import org.atypical.carabassa.core.model.impl.SearchCriteriaImpl;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.indexer.rdbms.configuration.IndexerRdbmsConfiguration;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.indexer.rdbms.test.configuration.TestConfiguration;
import org.atypical.carabassa.indexer.rdbms.test.helper.TestHelper;
import org.atypical.carabassa.storage.fs.configuration.StorageFSConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { CoreConfiguration.class, IndexerRdbmsConfiguration.class,
		StorageFSConfiguration.class, TestConfiguration.class })
@DataJpaTest
public class DatasetServiceTest {

	private static final String DATASET_TEST_NAME = "test1";

	@Autowired
	private DatasetService datasetService;

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void setUp() throws EntityExistsException, IOException {
		datasetService.deleteAll();
		datasetService.create(new DatasetImpl(DATASET_TEST_NAME));
		datasetService.create(new DatasetImpl("test2"));
	}

	@AfterEach
	void tearDown() throws IOException {
		datasetService.deleteAll();
	}

	@Test
	void addItemBlankFilename() throws IOException, EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertThrows(IllegalArgumentException.class,
				() -> datasetService.addItem(dataset, ItemType.IMAGE, null, new ByteArrayResource("test".getBytes())));
	}

	@Test
	void addItemExisting() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, TestHelper.getImageResource(FILENAME));

		// required to save item in db
		entityManager.flush();

		Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, finalDataset.getItems().size());

		assertThrows(EntityExistsException.class, () -> datasetService.addItem(finalDataset, ItemType.IMAGE, FILENAME,
				TestHelper.getImageResource(FILENAME)));
	}

	@Test
	void addItemInvalid() throws IOException, EntityNotFoundException {
		final String FILENAME = "IMG_INVALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		Resource resource = TestHelper.getImageResource(FILENAME);
		assertThrows(IOException.class, () -> datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, resource));
		assertEquals(0, dataset.getItems().size());
	}

	@Test
	void addItemInvalidTypeFile() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_INVALID_FILE_TYPE.gif";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedItem indexedItem = datasetService.addItem(dataset, ItemType.IMAGE, FILENAME,
				TestHelper.getImageResource(FILENAME));

		assertNotNull(indexedItem);
		assertEquals("jpg", indexedItem.getFormat());
	}

	@Test
	void addItemNotArchived() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_NO_DATE.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedItem indexedItem = datasetService.addItem(dataset, ItemType.IMAGE, FILENAME,
				TestHelper.getImageResource(FILENAME));

		assertNotNull(indexedItem);
		assertNotNull(indexedItem.getCreation());
		assertNull(indexedItem.getModification());
		assertNull(indexedItem.getArchiveTime());
		assertFalse(indexedItem.isArchived());
		assertEquals("jpg", indexedItem.getFormat());
		assertEquals(FILENAME, indexedItem.getFilename());
		assertNotNull(indexedItem.getHash());
		assertEquals("c90dc72d18cb6c62d8923fc2f276f94f", indexedItem.getHash());
		assertEquals(22, indexedItem.getTags().size());

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertEquals(1, dataset.getItems().size());

		indexedItem = dataset.getItems().iterator().next();

		assertNotNull(indexedItem);
		assertNotNull(indexedItem.getId());
		assertFalse(indexedItem.isArchived());
		assertNotNull(datasetService.findItemById(dataset, indexedItem.getId()));
		StoredItem storedItem = datasetService.getStoredItem(dataset, indexedItem);
		assertNotNull(storedItem);
		assertEquals(FILENAME, storedItem.getStoredItemInfo().getOriginalFilename());
	}

	@Test
	void addItemNullContent() throws IOException, EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		Resource resource = null;
		assertThrows(IllegalArgumentException.class,
				() -> datasetService.addItem(dataset, ItemType.IMAGE, "test", resource));
	}

	@Test
	void addItemSameDhash() throws IOException, EntityExistsException, EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		final String FILENAME1 = "IMG_DHASH_1.jpg";
		final String FILENAME2 = "IMG_DHASH_2.jpg";

		IndexedItem indexedItem1 = datasetService.addItem(dataset, ItemType.IMAGE, FILENAME1,
				TestHelper.getImageResource(FILENAME1));
		assertNotNull(indexedItem1);
		Tag perceptualTag1 = indexedItem1.getFirstTag(ImageMetadataTagger.TAG_DHASH);
		assertNotNull(perceptualTag1);

		IndexedItem indexedItem2 = datasetService.addItem(dataset, ItemType.IMAGE, FILENAME2,
				TestHelper.getImageResource(FILENAME2));
		assertNotNull(indexedItem2);
		Tag perceptualTag2 = indexedItem2.getFirstTag(ImageMetadataTagger.TAG_DHASH);
		assertNotNull(perceptualTag2);

		assertNotEquals(indexedItem1.getHash(), indexedItem2.getHash());
		assertEquals(perceptualTag1.getValue(String.class), perceptualTag2.getValue(String.class));
	}

	@Test
	void addItemTagInvalid() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_VALID.jpg";
		final String TAG_VALUE = "test";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, TestHelper.getImageResource(FILENAME));

		// required to save item in db
		entityManager.flush();

		final Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, finalDataset.getItems().size());
		IndexedItem indexedItem = finalDataset.getItems().iterator().next();

		assertThrows(IllegalArgumentException.class,
				() -> datasetService.addItemTag(finalDataset, indexedItem.getId(), null));

		Tag tag = new TagEntity(new TagImpl(null, TAG_VALUE));

		assertThrows(IllegalArgumentException.class,
				() -> datasetService.addItemTag(finalDataset, indexedItem.getId(), tag));
	}

	@Test
	void addItemTagOK() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_VALID.jpg";
		final String TAG_NAME = "meta.newTag";
		final String TAG_VALUE = "test";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, TestHelper.getImageResource(FILENAME));

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getItems().size());
		IndexedItem indexedItem = dataset.getItems().iterator().next();

		Tag tag = new TagEntity(new TagImpl(TAG_NAME, TAG_VALUE));
		Long tagId = datasetService.addItemTag(dataset, indexedItem.getId(), tag);
		assertNotNull(tagId);

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getItems().size());

		indexedItem = dataset.getItems().iterator().next();
		Set<Tag> tags = indexedItem.getTags(TAG_NAME);
		assertNotNull(tags);
		assertEquals(1, tags.size());

		tag = tags.iterator().next();
		assertEquals(TAG_VALUE, tag.getValue(String.class));
	}

	@Test
	void addItemValid() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedItem indexedItem = datasetService.addItem(dataset, ItemType.IMAGE, FILENAME,
				TestHelper.getImageResource(FILENAME));

		assertNotNull(indexedItem);
		assertEquals(ItemType.IMAGE, indexedItem.getType());
		assertNotNull(indexedItem.getCreation());
		assertNull(indexedItem.getModification());
		TestHelper.assertDateInUTC("2005-01-17T15:20:40", indexedItem.getArchiveTimeAsZoned("UTC"));
		assertTrue(indexedItem.isArchived());
		assertEquals("jpg", indexedItem.getFormat());
		assertEquals(FILENAME, indexedItem.getFilename());
		assertNotNull(indexedItem.getHash());
		assertEquals("f127c350588b861e813c45118b74aaec", indexedItem.getHash());
		assertEquals(87, indexedItem.getTags().size());

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getItems().size());

		indexedItem = datasetService.findItemById(dataset, indexedItem.getId());
		assertNotNull(indexedItem);
		assertNotNull(indexedItem.getId());
		TestHelper.assertDateInUTC("2005-01-17T15:20:40", indexedItem.getArchiveTimeAsZoned("UTC"));
		
		StoredItem storedItem = datasetService.getStoredItem(dataset, indexedItem);
		assertNotNull(storedItem);
		assertEquals(FILENAME, storedItem.getStoredItemInfo().getOriginalFilename());
	}

	@Test
	void createInvalidName() throws EntityExistsException, IOException {
		assertThrows(IllegalArgumentException.class, () -> datasetService.create(new DatasetImpl("Hi ")));

		assertThrows(IllegalArgumentException.class, () -> datasetService.create(new DatasetImpl("tmp/h")));

		assertNotNull(datasetService.create(new DatasetImpl("2020-01-11_Test")));
	}

	@Test
	void delete() throws IOException, EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertNotNull(dataset);
		datasetService.delete(dataset);
		assertThrows(EntityNotFoundException.class, () -> datasetService.findByName(DATASET_TEST_NAME));
	}

	@Test
	void deleteItemArchived() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, TestHelper.getImageResource(FILENAME));

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getItems().size());

		final IndexedItem indexedItem = dataset.getItems().iterator().next();
		assertNotNull(indexedItem);
		Long itemId = indexedItem.getId();
		assertNotNull(itemId);

		datasetService.deleteItem(dataset, itemId);

		// required to delete item in db
		entityManager.flush();

		final Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(0, finalDataset.getItems().size());

		assertThrows(EntityNotFoundException.class, () -> datasetService.findItemById(finalDataset, itemId));
		assertThrows(EntityNotFoundException.class, () -> datasetService.getStoredItem(finalDataset, indexedItem));
	}

	@Test
	void deleteItemNotArchived() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_NO_DATE.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, TestHelper.getImageResource(FILENAME));

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getItems().size());

		final IndexedItem finalIndexedItem = dataset.getItems().iterator().next();
		assertNotNull(finalIndexedItem);
		Long itemId = finalIndexedItem.getId();
		assertNotNull(itemId);

		datasetService.deleteItem(dataset, itemId);

		// required to delete item in db
		entityManager.flush();

		final Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(0, finalDataset.getItems().size());

		assertThrows(EntityNotFoundException.class, () -> datasetService.findItemById(finalDataset, itemId));
		assertThrows(EntityNotFoundException.class, () -> datasetService.getStoredItem(finalDataset, finalIndexedItem));
	}

	@Test
	void deleteItemTag() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		final String TAG_NAME = "meta.ExposureIndex";
		final String TAG_VALUE = "140/1";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, TestHelper.getImageResource(FILENAME));

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getItems().size());

		IndexedItem indexedItem = dataset.getItems().iterator().next();
		Set<Tag> tags = indexedItem.getTags(TAG_NAME);
		assertNotNull(tags);
		assertEquals(1, tags.size());

		Tag tag = tags.iterator().next();
		assertEquals(TAG_VALUE, tag.getValue(String.class));

		datasetService.deleteItemTag(dataset, indexedItem.getId(), tag.getId());

		// required to save item in db
		entityManager.flush();

		tags = indexedItem.getTags(TAG_NAME);
		assertNotNull(tags);
		assertEquals(0, tags.size());
	}

	@Test
	void deleteItemTagNotFound() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME, TestHelper.getImageResource(FILENAME));

		// required to save item in db
		entityManager.flush();

		Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, finalDataset.getItems().size());
		IndexedItem indexedItem = finalDataset.getItems().iterator().next();

		assertThrows(EntityNotFoundException.class,
				() -> datasetService.deleteItemTag(finalDataset, indexedItem.getId(), -1L));
	}

	@Test
	void findAll() {
		List<Dataset> datasets = datasetService.findAll();

		assertNotNull(datasets);
		assertEquals(2, datasets.size());
	}

	@Test
	void findById() throws EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertNotNull(dataset);

		dataset = datasetService.findById(dataset.getId());
		assertNotNull(dataset);
	}

	@Test
	void findByItemTagId() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_NO_DATE.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedItem indexedItem = datasetService.addItem(dataset, ItemType.IMAGE, FILENAME,
				TestHelper.getImageResource(FILENAME));

		assertNotNull(indexedItem);
		Tag hashTag = indexedItem.getFirstTag(Tagger.TAG_HASH);
		assertNotNull(hashTag);

		Long tagId = hashTag.getId();
		String hash = (String) hashTag.getValue();

		// required to save item in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertEquals(1, dataset.getItems().size());

		indexedItem = dataset.getItems().iterator().next();

		assertNotNull(indexedItem);
		assertNotNull(indexedItem.getId());

		Tag persistedHashTag = datasetService.findItemTagById(dataset, indexedItem.getId(), tagId);
		assertNotNull(persistedHashTag);
		assertEquals(hash, persistedHashTag.getValue());
	}

	@Test
	void findItems() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME1 = "IMG_VALID.jpg";
		final String FILENAME2 = "IMG_NO_DATE.jpg";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME1, TestHelper.getImageResource(FILENAME1));
		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME2, TestHelper.getImageResource(FILENAME2));

		// required to save item in db
		entityManager.flush();

		Page<IndexedItem> indexedItems = datasetService.findItems(dataset, PageRequest.of(0, 10));
		assertNotNull(indexedItems);
		assertEquals(2, indexedItems.getNumberOfElements());
		assertEquals(2, indexedItems.getTotalElements());

		indexedItems = datasetService.findItems(dataset, PageRequest.of(0, 1, Sort.by("hash").ascending()));
		assertNotNull(indexedItems);
		assertEquals(1, indexedItems.getNumberOfElements());
		assertEquals("c90dc72d18cb6c62d8923fc2f276f94f", indexedItems.getContent().get(0).getHash());
		assertEquals(2, indexedItems.getTotalElements());

		indexedItems = datasetService.findItems(dataset, PageRequest.of(1, 1));
		assertNotNull(indexedItems);
		assertEquals(1, indexedItems.getNumberOfElements());
		assertEquals("c90dc72d18cb6c62d8923fc2f276f94f", indexedItems.getContent().get(0).getHash());
		assertEquals(2, indexedItems.getTotalElements());
	}

	@Test
	void findItemsSearch() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME1 = "IMG_VALID.jpg";
		final String FILENAME2 = "IMG_NO_DATE.jpg";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME1, TestHelper.getImageResource(FILENAME1));
		datasetService.addItem(dataset, ItemType.IMAGE, FILENAME2, TestHelper.getImageResource(FILENAME2));

		// required to save item in db
		entityManager.flush();

		SearchCriteria searchCriteria = new SearchCriteriaImpl(
				new SearchConditionImpl("meta.YearCreated", SearchOperator.EQUAL, "2005"));
		Page<IndexedItem> indexedItems = datasetService.findItems(dataset, searchCriteria, PageRequest.of(0, 10));
		assertNotNull(indexedItems);
		assertEquals(1, indexedItems.getNumberOfElements());
		assertEquals(1, indexedItems.getTotalElements());
	}

	@Test
	void findByName() throws EntityNotFoundException {
		assertNotNull(datasetService.findByName(DATASET_TEST_NAME));
	}

	@Test
	void update() throws EntityNotFoundException, IOException {
		final String DESC = "new description!";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertNotNull(dataset.getCreation());
		assertNull(dataset.getModification());
		assertNull(dataset.getDescription());
		dataset.setDescription(DESC);
		Dataset updatedDataset = datasetService.update(dataset);

		// required to execute @PreUpdate on Dataset
		entityManager.flush();

		assertNotNull(updatedDataset.getCreation());
		assertNotNull(updatedDataset.getModification());
		assertNotNull(updatedDataset.getDescription());
		assertEquals(DESC, updatedDataset.getDescription());
	}

	@Test
	void updateChangeName() throws EntityNotFoundException, IOException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		dataset.setName("othername");

		assertThrows(IOException.class, () -> datasetService.update(dataset));
	}

	@Test
	void addItemNoDateOriginalButWithDateModified() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_20230115_151633.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedItem indexedItem = datasetService.addItem(dataset, ItemType.IMAGE, FILENAME,
				TestHelper.getImageResource(FILENAME));

		assertNotNull(indexedItem);
		assertNotNull(indexedItem.getCreation());
		assertNull(indexedItem.getModification());
		assertNotNull(indexedItem.getArchiveTime());
		assertFalse(indexedItem.isArchived());
		assertEquals("jpg", indexedItem.getFormat());
		assertEquals(FILENAME, indexedItem.getFilename());
		assertNotNull(indexedItem.getHash());
		assertEquals("78364f4c8712125fe370f2f9f469122c", indexedItem.getHash());
		assertEquals(45, indexedItem.getTags().size());
	}

}
