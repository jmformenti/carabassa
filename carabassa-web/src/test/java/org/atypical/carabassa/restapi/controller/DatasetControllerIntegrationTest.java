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
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.StoredImageImpl;
import org.atypical.carabassa.core.model.impl.StoredImageInfoImpl;
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
	void getImagesOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		Page<IndexedImage> page = new PageImpl<>(new ArrayList<>(dataset.getImages()));
		when(datasetService.findImages(isA(Dataset.class), isA(Pageable.class))).thenReturn(page);

		mvc.perform(get("/api/dataset/{datasetId}/image?page=0&size=10", DATASET_ID)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$._embedded.imageRepresentationList", hasSize(1))) //
				.andExpect(jsonPath("$._embedded.imageRepresentationList.[0].id", is(IMAGE_ID.intValue()))) //
				.andExpect(jsonPath("$.page.size", is(1))) //
				.andExpect(jsonPath("$.page.totalElements", is(1))) //
				.andExpect(jsonPath("$._links").exists()) //
				.andDo(log());
	}

	@Test
	void getImagesDatasetNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/image", DATASET_ID)) //
				.andExpect(status().isNotFound()).andDo(log());
	}

	@Test
	void getImageOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);

		mvc.perform(get("/api/dataset/{datasetId}/image/{imageId}", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.id", is(IMAGE_ID.intValue())))
				.andExpect(jsonPath("$.filename", is(indexedImage.getFilename())))
				.andExpect(jsonPath("$.fileType", is(indexedImage.getFileType())))
				.andExpect(jsonPath("$.hash", is(indexedImage.getHash())))
				.andExpect(jsonPath("$.creation",
						is(indexedImage.getCreation().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.modification",
						is(indexedImage.getModification().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.archiveTime",
						is(indexedImage.getArchiveTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
				.andExpect(jsonPath("$.tags", hasSize(1))).andExpect(jsonPath("$.tags[0].id", is(TAG_ID.intValue())))
				.andExpect(jsonPath("$.tags[0].name", is(tag.getName())))
				.andExpect(jsonPath("$.tags[0].value", is(tag.getValue(String.class))))
				.andExpect(jsonPath("$.tags[0].boundingBox.minX", is(tag.getBoundingBox().getMinX())))
				.andExpect(jsonPath("$.tags[0].boundingBox.minY", is(tag.getBoundingBox().getMinY())))
				.andExpect(jsonPath("$.tags[0].boundingBox.width", is(tag.getBoundingBox().getWidth())))
				.andExpect(jsonPath("$.tags[0].boundingBox.height", is(tag.getBoundingBox().getHeight()))).andDo(log());
	}

	@Test
	void getImageDatasetNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/image/{imageId}", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void getImageNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/image/{imageId}", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void existsImageOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageByHash(dataset, IMAGE_HASH)).thenReturn(indexedImage);

		mvc.perform(get("/api/dataset/{datasetId}/image/exists/{hash}", DATASET_ID, IMAGE_HASH)) //
				.andExpect(status().isOk()) //
				.andDo(log());
	}

	@Test
	void existsImageNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageByHash(dataset, IMAGE_HASH)).thenThrow(EntityNotFoundException.class);

		mvc.perform(get("/api/dataset/{datasetId}/image/exists/{hash}", DATASET_ID, IMAGE_HASH)) //
				.andExpect(status().isNotFound()) //
				.andDo(log());
	}

	@Test
	void getImageContentOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);

		StoredImage storedImage = new StoredImageImpl();
		storedImage.setStoredImageInfo(new StoredImageInfoImpl("test.jpg"));
		storedImage.setContent("test".getBytes());
		when(datasetService.getStoredImage(dataset, indexedImage)).thenReturn(storedImage);

		mvc.perform(get("/api/dataset/{datasetId}/image/{imageId}/content", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentType("image/" + indexedImage.getFileType()))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + indexedImage.getFilename() + "\""))
				.andExpect(content().string("test")) //
				.andDo(log());
	}

	@Test
	void addImageOK() throws Exception {
		final byte[] CONTENT = "test".getBytes();

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addImage(isA(Dataset.class), isA(Resource.class))).thenReturn(indexedImage);

		MockMultipartFile file = new MockMultipartFile("file", indexedImage.getFilename(), null, CONTENT);

		mvc.perform(multipart("/api/dataset/{datasetId}/image", DATASET_ID).file(file)) //
				.andExpect(status().isCreated()) //
				.andExpect(jsonPath("$.id", is(indexedImage.getId().intValue()))) //
				.andDo(log());
	}

	@Test
	void addImageExisting() throws Exception {
		final byte[] CONTENT = "test".getBytes();

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addImage(isA(Dataset.class), isA(Resource.class))).thenThrow(EntityExistsException.class);

		MockMultipartFile file = new MockMultipartFile("file", indexedImage.getFilename(), null, CONTENT);

		mvc.perform(multipart("/api/dataset/{datasetId}/image", DATASET_ID).file(file)) //
				.andExpect(status().isConflict()) //
				.andDo(log());
	}

	@Test
	void addImageInvalid() throws Exception {
		final byte[] CONTENT = "test".getBytes();

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addImage(isA(Dataset.class), isA(Resource.class)))
				.thenThrow(IllegalArgumentException.class);

		MockMultipartFile file = new MockMultipartFile("file", indexedImage.getFilename(), null, CONTENT);

		mvc.perform(multipart("/api/dataset/{datasetId}/image", DATASET_ID).file(file)) //
				.andExpect(status().isConflict()) //
				.andDo(log());
	}

	@Test
	void addImageTagOK() throws Exception {
		String json = objectMapper
				.writeValueAsString(new TagEntityRepresentation(TAG_ID, tag.getName(), tag.getValue()));

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		when(datasetService.addImageTag(isA(Dataset.class), isA(Long.class), isA(Tag.class))).thenReturn(TAG_ID);

		mvc.perform(post("/api/dataset/{datasetId}/image/{imageId}/tag", DATASET_ID, IMAGE_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isCreated()) //
				.andExpect(jsonPath("$.id", is(TAG_ID.intValue()))) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void addImageTagInvalid() throws Exception {
		String json = objectMapper.writeValueAsString(new TagEntityRepresentation(TAG_ID, null, tag.getValue()));

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		when(datasetService.addImageTag(isA(Dataset.class), isA(Long.class), isA(Tag.class)))
				.thenThrow(IllegalArgumentException.class);

		mvc.perform(post("/api/dataset/{datasetId}/image/{imageId}/tag", DATASET_ID, IMAGE_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isBadRequest()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteImageOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		doNothing().when(datasetService).deleteImage(dataset, IMAGE_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/image/{imageId}", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteImageNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		doNothing().when(datasetService).deleteImage(dataset, IMAGE_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/image/{imageId}", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteImageTagOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		doNothing().when(datasetService).deleteImageTag(dataset, IMAGE_ID, TAG_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/image/{imageId}/tag/{tagId}", DATASET_ID, IMAGE_ID, TAG_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(MockMvcResultHandlers.log());
	}

	@Test
	void deleteImageTagNotFound() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		doThrow(EntityNotFoundException.class).when(datasetService).deleteImageTag(dataset, IMAGE_ID, TAG_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/image/{imageId}/tag/{tagId}", DATASET_ID, IMAGE_ID, TAG_ID)) //
				.andExpect(status().isNotFound()) //
				.andDo(MockMvcResultHandlers.log());
	}

}
