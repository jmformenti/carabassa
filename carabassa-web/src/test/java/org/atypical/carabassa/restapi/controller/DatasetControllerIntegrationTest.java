package org.atypical.carabassa.restapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.impl.StoredItemImpl;
import org.atypical.carabassa.core.model.impl.StoredItemInfoImpl;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.rdbms.configuration.RestApiRdbmsMapperConfiguration;
import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;
import org.atypical.carabassa.restapi.test.helper.DatasetControllerHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@ContextConfiguration(classes = { RestApiConfiguration.class, RestApiRdbmsMapperConfiguration.class })
@WebMvcTest(DatasetController.class)
public class DatasetControllerIntegrationTest extends DatasetControllerHelper {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private DatasetService datasetService;

	@BeforeEach
	void setUp() throws EntityExistsException {
		super.initData();
	}

	@Test
	void createOK() throws Exception {
		String json = objectMapper.writeValueAsString(new DatasetEntityRepresentation(DATASET_NAME));

		when(datasetService.create(isA(Dataset.class))).thenReturn(dataset);

		mvc.perform(post("/api/dataset") //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isCreated()) //
				.andExpect(jsonPath("$.id", is(DATASET_ID.intValue()))) //
				.andDo(log());
	}

	@Test
	void createAlreadyExists() throws Exception {
		String json = objectMapper.writeValueAsString(new DatasetEditableRepresentation(DATASET_NAME, "desc"));

		when(datasetService.create(isA(Dataset.class))).thenThrow(new EntityExistsException());

		mvc.perform(post("/api/dataset") //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isConflict()) //
				.andDo(log());
	}

	@Test
	void createInvalid() throws Exception {
		String json = objectMapper.writeValueAsString(new DatasetEditableRepresentation(null, "desc"));

		when(datasetService.create(isA(Dataset.class))).thenThrow(IllegalArgumentException.class);

		mvc.perform(post("/api/dataset") //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isBadRequest()) //
				.andDo(log());
	}

	@Test
	void deleteOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		doNothing().when(datasetService).delete(dataset);

		mvc.perform(delete("/api/dataset/{datasetId}", DATASET_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(log());
	}

	@Test
	void deleteNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(delete("/api/dataset/{datasetId}", DATASET_ID)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void findAllOK() throws Exception {
		Page<Dataset> page = new PageImpl<>(Arrays.asList(dataset));

		when(datasetService.findAll(isA(Pageable.class))).thenReturn(page);

		mvc.perform(get("/api/dataset?page=0&size=10")) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$._embedded.datasetEntityRepresentationList", hasSize(1))) //
				.andExpect(jsonPath("$._embedded.datasetEntityRepresentationList.[0].name", is(dataset.getName()))) //
				.andExpect(jsonPath("$.page.size", is(1))) //
				.andExpect(jsonPath("$.page.totalElements", is(1))) //
				.andExpect(jsonPath("$._links").exists()) //
				.andDo(log());
	}

	@Test
	void findByIdOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);

		mvc.perform(get("/api/dataset/{datasetId}", DATASET_ID)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.id", is(DATASET_ID.intValue()))) //
				.andExpect(jsonPath("$.name", is(DATASET_NAME))) //
				.andExpect(jsonPath("$.description", is(dataset.getDescription())))
				.andExpect(jsonPath("$.creation",
						is(dataset.getCreation().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.modification",
						is(dataset.getModification().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andDo(log());
	}

	@Test
	void findByIdNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID + 1)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}", DATASET_ID + 1)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void findByNameOK() throws Exception {
		when(datasetService.findByName(DATASET_NAME)).thenReturn(dataset);

		mvc.perform(get("/api/dataset/name/{datasetName}", DATASET_NAME)) //
				.andExpect(status().isOk()).andExpect(jsonPath("$.id", is(DATASET_ID.intValue())))
				.andExpect(jsonPath("$.name", is(DATASET_NAME)))
				.andExpect(jsonPath("$.description", is(dataset.getDescription())))
				.andExpect(jsonPath("$.creation",
						is(dataset.getCreation().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.modification",
						is(dataset.getModification().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andDo(log());
	}

	@Test
	void findByNameNotFound() throws Exception {
		when(datasetService.findByName(DATASET_NAME + "1")).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/name/{datasetName}", DATASET_NAME + "1")) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void updateOK() throws Exception {
		String json = objectMapper.writeValueAsString(new DatasetEntityRepresentation(DATASET_NAME));

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.update(dataset)).thenReturn(dataset);

		mvc.perform(put("/api/dataset/{datasetId}", DATASET_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isNoContent()) //
				.andDo(log());
	}

	@Test
	void updateNotFound() throws Exception {
		String json = objectMapper.writeValueAsString(new DatasetEntityRepresentation(DATASET_NAME));

		when(datasetService.findById(DATASET_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(put("/api/dataset/{datasetId}", DATASET_ID) //
				.contentType(MediaType.APPLICATION_JSON) //
				.content(json)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void getItemsOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		Page<IndexedItem> page = new PageImpl<>(new ArrayList<>(dataset.getItems()));
		when(datasetService.findItems(isA(Dataset.class), isA(Pageable.class))).thenReturn(page);

		mvc.perform(get("/api/dataset/{datasetId}/item?page=0&size=10", DATASET_ID)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$._embedded.itemRepresentationList", hasSize(1))) //
				.andExpect(jsonPath("$._embedded.itemRepresentationList.[0].id", is(ITEM_ID.intValue()))) //
				.andExpect(jsonPath("$.page.size", is(1))) //
				.andExpect(jsonPath("$.page.totalElements", is(1))) //
				.andExpect(jsonPath("$._links").exists()) //
				.andDo(log());
	}

	@Test
	void getItemsDatasetNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/item", DATASET_ID)) //
				.andExpect(status().isNotFound()).andDo(log());
	}

	@Test
	void getItemOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);

		mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.id", is(ITEM_ID.intValue())))
				.andExpect(jsonPath("$.type", is(indexedItem.getType().normalized())))
				.andExpect(jsonPath("$.filename", is(indexedItem.getFilename())))
				.andExpect(jsonPath("$.format", is(indexedItem.getFormat())))
				.andExpect(jsonPath("$.hash", is(indexedItem.getHash())))
				.andExpect(jsonPath("$.creation",
						is(indexedItem.getCreation().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.modification",
						is(indexedItem.getModification().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.archiveTime",
						is(indexedItem.getArchiveTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.tags", hasSize(1))).andExpect(jsonPath("$.tags[0].id", is(TAG_ID.intValue())))
				.andExpect(jsonPath("$.tags[0].name", is(tag.getName())))
				.andExpect(jsonPath("$.tags[0].value", is(tag.getValue(String.class))))
				.andExpect(jsonPath("$.tags[0].boundingBox.minX", is(tag.getBoundingBox().getMinX())))
				.andExpect(jsonPath("$.tags[0].boundingBox.minY", is(tag.getBoundingBox().getMinY())))
				.andExpect(jsonPath("$.tags[0].boundingBox.width", is(tag.getBoundingBox().getWidth())))
				.andExpect(jsonPath("$.tags[0].boundingBox.height", is(tag.getBoundingBox().getHeight()))).andDo(log());
	}

	@Test
	void getItemDatasetNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void getItemNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void existsItemOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemByHash(dataset, ITEM_HASH)).thenReturn(indexedItem);

		mvc.perform(get("/api/dataset/{datasetId}/item/exists/{hash}", DATASET_ID, ITEM_HASH)) //
				.andExpect(status().isOk()) //
				.andDo(log());
	}

	@Test
	void existsItemNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemByHash(dataset, ITEM_HASH)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/item/exists/{hash}", DATASET_ID, ITEM_HASH)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void getItemContentOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);

		StoredItem storedItem = new StoredItemImpl();
		storedItem.setStoredItemInfo(new StoredItemInfoImpl("test.jpg"));
		storedItem.setContent("test".getBytes());
		when(datasetService.getStoredItem(dataset, indexedItem)).thenReturn(storedItem);

		mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}/content", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentType(indexedItem.getType().name() + "/" + indexedItem.getFormat()))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + indexedItem.getFilename() + "\""))
				.andExpect(content().string("test")) //
				.andDo(log());
	}

	@Test
	void addItemOK() throws Exception {
		final byte[] CONTENT = "test".getBytes();

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addItem(isA(Dataset.class), isA(ItemType.class), isA(String.class), isA(Resource.class)))
				.thenReturn(indexedItem);

		MockMultipartFile file = new MockMultipartFile("file", indexedItem.getFilename(), "image/jpg", CONTENT);

		mvc.perform(multipart("/api/dataset/{datasetId}/item", DATASET_ID).file(file)) //
				.andExpect(status().isCreated()) //
				.andExpect(jsonPath("$.id", is(indexedItem.getId().intValue()))) //
				.andDo(log());
	}

	@Test
	void addItemExisting() throws Exception {
		final byte[] CONTENT = "test".getBytes();

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addItem(isA(Dataset.class), isA(ItemType.class), isA(String.class), isA(Resource.class)))
				.thenThrow(EntityExistsException.class);

		MockMultipartFile file = new MockMultipartFile("file", indexedItem.getFilename(), "image/jpg", CONTENT);

		mvc.perform(multipart("/api/dataset/{datasetId}/item", DATASET_ID).file(file)) //
				.andExpect(status().isConflict()) //
				.andDo(log());
	}

	@Test
	void addItemInvalid() throws Exception {
		final byte[] CONTENT = "test".getBytes();

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addItem(isA(Dataset.class), isA(ItemType.class), isA(String.class), isA(Resource.class)))
				.thenThrow(IllegalArgumentException.class);

		MockMultipartFile file = new MockMultipartFile("file", indexedItem.getFilename(), "image/jpg", CONTENT);

		mvc.perform(multipart("/api/dataset/{datasetId}/item", DATASET_ID).file(file)) //
				.andExpect(status().isConflict()) //
				.andDo(log());
	}

	@Test
	void addItemTagOK() throws Exception {
		String json = objectMapper
				.writeValueAsString(new TagEntityRepresentation(TAG_ID, tag.getName(), tag.getValue()));

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		when(datasetService.addItemTag(isA(Dataset.class), isA(Long.class), isA(Tag.class))).thenReturn(TAG_ID);

		mvc.perform(post("/api/dataset/{datasetId}/item/{itemId}/tag", DATASET_ID, ITEM_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isCreated()) //
				.andExpect(jsonPath("$.id", is(TAG_ID.intValue()))) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void addItemTagInvalid() throws Exception {
		String json = objectMapper.writeValueAsString(new TagEntityRepresentation(TAG_ID, null, tag.getValue()));

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		when(datasetService.addItemTag(isA(Dataset.class), isA(Long.class), isA(Tag.class)))
				.thenThrow(IllegalArgumentException.class);

		mvc.perform(post("/api/dataset/{datasetId}/item/{itemId}/tag", DATASET_ID, ITEM_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isBadRequest()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteItemOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		doNothing().when(datasetService).deleteItem(dataset, ITEM_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteItemNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		doNothing().when(datasetService).deleteItem(dataset, ITEM_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteItemTagOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		doNothing().when(datasetService).deleteItemTag(dataset, ITEM_ID, TAG_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}/tag/{tagId}", DATASET_ID, ITEM_ID, TAG_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteItemTagNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		doThrow(EntityNotFoundException.class).when(datasetService).deleteItemTag(dataset, ITEM_ID, TAG_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}/tag/{tagId}", DATASET_ID, ITEM_ID, TAG_ID)) //
				.andExpect(status().isNotFound()) //
				.andDo(MockMvcResultHandlers.log());
	}

}
