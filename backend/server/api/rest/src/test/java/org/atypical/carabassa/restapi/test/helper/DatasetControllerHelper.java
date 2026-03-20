package org.atypical.carabassa.restapi.test.helper;

import tools.jackson.databind.ObjectMapper;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.impl.BoundingBoxImpl;
import org.atypical.carabassa.core.model.impl.DatasetImpl;
import org.atypical.carabassa.core.model.impl.IndexedItemImpl;
import org.atypical.carabassa.core.model.impl.TagImpl;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.TreeSet;

public class DatasetControllerHelper {

    protected final static Long DATASET_ID = 1L;
    protected final static Long ITEM_ID = 2L;
    protected final static Long TAG_ID = 3L;
    protected final static String ITEM_HASH = "hash";

    protected final static String DATASET_NAME = "mydataset";
    protected final static String TAG_NAME = "tag_name";
    protected final static String TAG_VALUE = "tag_value";

    protected Dataset dataset;
    protected DatasetEntityRepresentation datasetRepresentation;
    protected IndexedItem indexedItem;
    protected ItemRepresentation itemRepresentation;
    protected Tag tag;
    protected TagEntityRepresentation tagRepresentation;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected void initData() {
        Instant now = Instant.now();
        Instant dayBefore = now.minus(Duration.ofDays(1));

        dataset = new DatasetImpl(DATASET_NAME);
        dataset.setId(DATASET_ID);
        dataset.setDescription("description");
        dataset.setCreation(dayBefore);
        dataset.setModification(now);

        datasetRepresentation = new DatasetEntityRepresentation(DATASET_NAME);
        datasetRepresentation.setId(DATASET_ID);
        datasetRepresentation.setDescription("description");
        datasetRepresentation.setCreation(dayBefore);
        datasetRepresentation.setModification(dayBefore);

        tag = new TagImpl(TAG_NAME, TAG_VALUE);
        tag.setId(TAG_ID);
        tag.setBoundingBox(new BoundingBoxImpl(1, 2, 3, 4));

        tagRepresentation = new TagEntityRepresentation(TAG_ID, TAG_NAME, TAG_VALUE);

        indexedItem = new IndexedItemImpl();
        indexedItem.setId(ITEM_ID);
        indexedItem.setType(ItemType.IMAGE);
        indexedItem.setFilename("test.jpg");
        indexedItem.setFormat("jpg");
        indexedItem.setHash("12345");
        indexedItem.setCreation(now);
        indexedItem.setModification(now);
        indexedItem.setArchiveTime(Instant.now());
        indexedItem.setTags(new HashSet<>());
        indexedItem.getTags().add(new TagImpl(tag));

        itemRepresentation = new ItemRepresentation();
        itemRepresentation.setId(ITEM_ID);
        indexedItem.setType(ItemType.IMAGE);
        itemRepresentation.setFilename("test.jpg");
        itemRepresentation.setFormat("jpg");
        itemRepresentation.setHash("12345");
        itemRepresentation.setCreation(now);
        itemRepresentation.setModification(now);
        itemRepresentation.setArchiveTime(Instant.now());
        itemRepresentation.setTags(new TreeSet<>());
        itemRepresentation.getTags().add(tagRepresentation);

        dataset.getItems().add(indexedItem);
    }

}
