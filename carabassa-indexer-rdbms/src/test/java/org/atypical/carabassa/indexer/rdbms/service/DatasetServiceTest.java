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

import org.atypical.carabassa.core.component.tagger.impl.ImageMetadataTagger;
import org.atypical.carabassa.core.configuration.CoreConfiguration;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.DatasetImpl;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.indexer.rdbms.configuration.IndexerRdbmsConfiguration;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.indexer.rdbms.test.configuration.TestConfiguration;
import org.atypical.carabassa.indexer.rdbms.test.helper.TestHelper;
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

@ContextConfiguration(classes = { CoreConfiguration.class, IndexerRdbmsConfiguration.class, TestConfiguration.class })
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
	void addImageBlankFilename() throws IOException, EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertThrows(IllegalArgumentException.class,
				() -> datasetService.addImage(dataset, new ByteArrayResource("test".getBytes())));
	}

	@Test
	void addImageExisting() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		// required to save image in db
		entityManager.flush();

		Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, finalDataset.getImages().size());

		assertThrows(EntityExistsException.class,
				() -> datasetService.addImage(finalDataset, TestHelper.getImageResource(FILENAME)));
	}

	@Test
	void addImageInvalid() throws IOException, EntityNotFoundException {
		final String FILENAME = "IMG_INVALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		Resource resource = TestHelper.getImageResource(FILENAME);
		assertThrows(IOException.class, () -> datasetService.addImage(dataset, resource));
		assertEquals(0, dataset.getImages().size());
	}

	@Test
	void addImageInvalidTypeFile() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_INVALID_FILE_TYPE.gif";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedImage image = datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		assertNotNull(image);
		assertEquals("jpg", image.getFileType());
	}

	@Test
	void addImageNotArchived() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_NO_DATE.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedImage image = datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		assertNotNull(image);
		assertNotNull(image.getCreation());
		assertNull(image.getModification());
		assertNull(image.getArchiveTime());
		assertFalse(image.isArchived());
		assertEquals("jpg", image.getFileType());
		assertEquals(FILENAME, image.getFilename());
		assertNotNull(image.getHash());
		assertEquals("c90dc72d18cb6c62d8923fc2f276f94f", image.getHash());
		Tag perceptualTag = image.getFirstTag(ImageMetadataTagger.TAG_DHASH);
		assertNotNull(perceptualTag);
		assertEquals("98580d54aa16305731421c3de555680d505f", perceptualTag.getValue(String.class));
		assertEquals(22, image.getTags().size());

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertEquals(1, dataset.getImages().size());

		image = dataset.getImages().iterator().next();

		assertNotNull(image);
		assertNotNull(image.getId());
		assertFalse(image.isArchived());
		assertNotNull(datasetService.findImageById(dataset, image.getId()));
		StoredImage storedImage = datasetService.getStoredImage(dataset, image);
		assertNotNull(storedImage);
		assertEquals(FILENAME, storedImage.getStoredImageInfo().getOriginalFilename());
	}

	@Test
	void addImageNullContent() throws IOException, EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertThrows(IllegalArgumentException.class, () -> datasetService.addImage(dataset, null));
	}

	@Test
	void addImageSameDhash() throws IOException, EntityExistsException, EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		final String FILENAME1 = "IMG_DHASH_1.jpg";
		final String FILENAME2 = "IMG_DHASH_2.jpg";

		IndexedImage image1 = datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME1));
		assertNotNull(image1);
		Tag perceptualTag1 = image1.getFirstTag(ImageMetadataTagger.TAG_DHASH);
		assertNotNull(perceptualTag1);

		IndexedImage image2 = datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME2));
		assertNotNull(image2);
		Tag perceptualTag2 = image2.getFirstTag(ImageMetadataTagger.TAG_DHASH);
		assertNotNull(perceptualTag2);

		assertNotEquals(image1.getHash(), image2.getHash());
		assertEquals(perceptualTag1.getValue(String.class), perceptualTag2.getValue(String.class));
	}

	@Test
	void addImageTagInvalid() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_VALID.jpg";
		final String TAG_VALUE = "test";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		// required to save image in db
		entityManager.flush();

		final Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, finalDataset.getImages().size());
		IndexedImage image = finalDataset.getImages().iterator().next();

		assertThrows(IllegalArgumentException.class,
				() -> datasetService.addImageTag(finalDataset, image.getId(), null));

		Tag tag = new TagEntity(new TagImpl(null, TAG_VALUE));

		assertThrows(IllegalArgumentException.class,
				() -> datasetService.addImageTag(finalDataset, image.getId(), tag));
	}

	@Test
	void addImageTagOK() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_VALID.jpg";
		final String TAG_NAME = "meta.newTag";
		final String TAG_VALUE = "test";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getImages().size());
		IndexedImage image = dataset.getImages().iterator().next();

		Tag tag = new TagEntity(new TagImpl(TAG_NAME, TAG_VALUE));
		Long tagId = datasetService.addImageTag(dataset, image.getId(), tag);
		assertNotNull(tagId);

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getImages().size());

		image = dataset.getImages().iterator().next();
		Set<Tag> tags = image.getTags(TAG_NAME);
		assertNotNull(tags);
		assertEquals(1, tags.size());

		tag = tags.iterator().next();
		assertEquals(TAG_VALUE, tag.getValue(String.class));
	}

	@Test
	void addImageValid() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedImage image = datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		assertNotNull(image);
		assertNotNull(image.getCreation());
		assertNull(image.getModification());
		TestHelper.assertDateInUTC("2005-01-17T16:20:40", image.getArchiveTime());
		assertTrue(image.isArchived());
		assertEquals("jpg", image.getFileType());
		assertEquals(FILENAME, image.getFilename());
		assertNotNull(image.getHash());
		assertEquals("f127c350588b861e813c45118b74aaec", image.getHash());
		Tag perceptualTag = image.getFirstTag(ImageMetadataTagger.TAG_DHASH);
		assertNotNull(perceptualTag);
		assertEquals("05102a1abe24f4e2dbdffc7dd5847ffc18a0", perceptualTag.getValue(String.class));
		assertEquals(87, image.getTags().size());

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertEquals(1, dataset.getImages().size());

		image = dataset.getImages().iterator().next();

		assertNotNull(image);
		assertNotNull(image.getId());
		assertNotNull(datasetService.findImageById(dataset, image.getId()));
		StoredImage storedImage = datasetService.getStoredImage(dataset, image);
		assertNotNull(storedImage);
		assertEquals(FILENAME, storedImage.getStoredImageInfo().getOriginalFilename());
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
	void deleteImageArchived() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getImages().size());

		final IndexedImage image = dataset.getImages().iterator().next();
		assertNotNull(image);
		Long imageId = image.getId();
		assertNotNull(imageId);

		datasetService.deleteImage(dataset, imageId);

		// required to delete image in db
		entityManager.flush();

		final Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(0, finalDataset.getImages().size());

		assertThrows(EntityNotFoundException.class, () -> datasetService.findImageById(finalDataset, imageId));
		assertThrows(EntityNotFoundException.class, () -> datasetService.getStoredImage(finalDataset, image));
	}

	@Test
	void deleteImageNotArchived() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_NO_DATE.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getImages().size());

		final IndexedImage finalImage = dataset.getImages().iterator().next();
		assertNotNull(finalImage);
		Long imageId = finalImage.getId();
		assertNotNull(imageId);

		datasetService.deleteImage(dataset, imageId);

		// required to delete image in db
		entityManager.flush();

		final Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(0, finalDataset.getImages().size());

		assertThrows(EntityNotFoundException.class, () -> datasetService.findImageById(finalDataset, imageId));
		assertThrows(EntityNotFoundException.class, () -> datasetService.getStoredImage(finalDataset, finalImage));
	}

	@Test
	void deleteImageTag() throws IOException, EntityExistsException, EntityNotFoundException {
		final String FILENAME = "IMG_VALID.jpg";
		final String TAG_NAME = "meta.ExposureIndex";
		final String TAG_VALUE = "140/1";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, dataset.getImages().size());

		IndexedImage image = dataset.getImages().iterator().next();
		Set<Tag> tags = image.getTags(TAG_NAME);
		assertNotNull(tags);
		assertEquals(1, tags.size());

		Tag tag = tags.iterator().next();
		assertEquals(TAG_VALUE, tag.getValue(String.class));

		datasetService.deleteImageTag(dataset, image.getId(), tag.getId());

		// required to save image in db
		entityManager.flush();

		tags = image.getTags(TAG_NAME);
		assertNotNull(tags);
		assertEquals(0, tags.size());
	}

	@Test
	void deleteImageTagNotFound() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_VALID.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		// required to save image in db
		entityManager.flush();

		Dataset finalDataset = datasetService.findByName(DATASET_TEST_NAME);
		assertEquals(1, finalDataset.getImages().size());
		IndexedImage image = finalDataset.getImages().iterator().next();

		assertThrows(EntityNotFoundException.class,
				() -> datasetService.deleteImageTag(finalDataset, image.getId(), -1L));
	}

	@Test
	void findAll() {
		List<Dataset> datasets = datasetService.findAll();

		assertNotNull(datasets);
		assertEquals(2, datasets.size());
	}

	@Test
	void findByImageId() throws EntityNotFoundException {
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		assertNotNull(dataset);

		dataset = datasetService.findById(dataset.getId());
		assertNotNull(dataset);
	}

	@Test
	void findByImageTagId() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME = "IMG_NO_DATE.jpg";
		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);

		IndexedImage image = datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME));

		assertNotNull(image);
		Tag perceptualTag = image.getFirstTag(ImageMetadataTagger.TAG_DHASH);
		assertNotNull(perceptualTag);

		Long tagId = perceptualTag.getId();
		String dhash = (String) perceptualTag.getValue();

		// required to save image in db
		entityManager.flush();

		dataset = datasetService.findByName(DATASET_TEST_NAME);

		assertEquals(1, dataset.getImages().size());

		image = dataset.getImages().iterator().next();

		assertNotNull(image);
		assertNotNull(image.getId());

		Tag perceptualPersistedTag = datasetService.findImageTagById(dataset, image.getId(), tagId);
		assertNotNull(perceptualPersistedTag);
		assertEquals(dhash, perceptualPersistedTag.getValue());
	}

	@Test
	void findImages() throws EntityNotFoundException, IOException, EntityExistsException {
		final String FILENAME1 = "IMG_VALID.jpg";
		final String FILENAME2 = "IMG_NO_DATE.jpg";

		Dataset dataset = datasetService.findByName(DATASET_TEST_NAME);
		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME1));
		datasetService.addImage(dataset, TestHelper.getImageResource(FILENAME2));

		// required to save image in db
		entityManager.flush();

		Page<IndexedImage> indexedImages = datasetService.findImages(dataset, PageRequest.of(0, 10));
		assertNotNull(indexedImages);
		assertEquals(2, indexedImages.getNumberOfElements());
		assertEquals(2, indexedImages.getTotalElements());

		indexedImages = datasetService.findImages(dataset, PageRequest.of(0, 1, Sort.by("hash").ascending()));
		assertNotNull(indexedImages);
		assertEquals(1, indexedImages.getNumberOfElements());
		assertEquals("c90dc72d18cb6c62d8923fc2f276f94f", indexedImages.getContent().get(0).getHash());
		assertEquals(2, indexedImages.getTotalElements());

		indexedImages = datasetService.findImages(dataset, PageRequest.of(1, 1));
		assertNotNull(indexedImages);
		assertEquals(1, indexedImages.getNumberOfElements());
		assertEquals("c90dc72d18cb6c62d8923fc2f276f94f", indexedImages.getContent().get(0).getHash());
		assertEquals(2, indexedImages.getTotalElements());
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

}
