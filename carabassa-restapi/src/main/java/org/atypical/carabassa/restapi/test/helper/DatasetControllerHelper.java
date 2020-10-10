package org.atypical.carabassa.restapi.test.helper;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.TreeSet;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.BoundingBoxImpl;
import org.atypical.carabassa.core.model.impl.DatasetImpl;
import org.atypical.carabassa.core.model.impl.IndexedImageImpl;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DatasetControllerHelper {

	protected final static Long DATASET_ID = 1L;
	protected final static Long IMAGE_ID = 2L;
	protected final static Long TAG_ID = 3L;
	protected final static String IMAGE_HASH = "hash";

	protected final static String DATASET_NAME = "mydataset";
	protected final static String TAG_NAME = "tag_name";
	protected final static String TAG_VALUE = "tag_value";

	protected Dataset dataset;
	protected DatasetEntityRepresentation datasetRepresentation;
	protected IndexedImage indexedImage;
	protected ImageRepresentation imageRepresentation;
	protected Tag tag;
	protected TagEntityRepresentation tagRepresentation;

	protected ObjectMapper objectMapper = new ObjectMapper();

	protected void initData() {
		ZonedDateTime now = ZonedDateTime.now();

		dataset = new DatasetImpl(DATASET_NAME);
		dataset.setId(DATASET_ID);
		dataset.setDescription("description");
		dataset.setCreation(ZonedDateTime.now().minusDays(1));
		dataset.setModification(now);

		datasetRepresentation = new DatasetEntityRepresentation(DATASET_NAME);
		datasetRepresentation.setId(DATASET_ID);
		datasetRepresentation.setDescription("description");
		datasetRepresentation.setCreation(ZonedDateTime.now().minusDays(1));
		datasetRepresentation.setModification(ZonedDateTime.now());

		tag = new TagImpl(TAG_NAME, TAG_VALUE);
		tag.setId(TAG_ID);
		tag.setBoundingBox(new BoundingBoxImpl(1, 2, 3, 4));

		tagRepresentation = new TagEntityRepresentation(TAG_ID, TAG_NAME, TAG_VALUE);

		indexedImage = new IndexedImageImpl();
		indexedImage.setId(IMAGE_ID);
		indexedImage.setFilename("test.jpg");
		indexedImage.setFileType("jpg");
		indexedImage.setHash("12345");
		indexedImage.setCreation(now);
		indexedImage.setModification(now);
		indexedImage.setArchiveTime(now);
		indexedImage.setTags(new HashSet<>());
		indexedImage.getTags().add(new TagImpl(tag));

		imageRepresentation = new ImageRepresentation();
		imageRepresentation.setId(IMAGE_ID);
		imageRepresentation.setFilename("test.jpg");
		imageRepresentation.setFileType("jpg");
		imageRepresentation.setHash("12345");
		imageRepresentation.setCreation(now);
		imageRepresentation.setModification(now);
		imageRepresentation.setArchiveTime(now);
		imageRepresentation.setTags(new TreeSet<>());
		imageRepresentation.getTags().add(tagRepresentation);

		dataset.getImages().add(indexedImage);
	}

}
