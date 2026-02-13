package org.atypical.carabassa.web;

import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatasetControllerIT {

    private final String FILE_NAME = "IMG_VALID.jpg";
    private final static String ITEM_HASH = "f127c350588b861e813c45118b74aaec";
    private final static String DATASET_NAME = "dataset_name";
    private final static String DATASET_DESC = "dataset_desc";
    private final static Long TAG_ID = 3L;
    private final static String TAG_NAME = "tag_name";
    private final static String TAG_VALUE = "tag_value";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DatasetService datasetService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() throws IOException {
        datasetService.deleteAll();
    }

    @Test
    void createOK() throws Exception {
        createDataset();
    }

    @Test
    void createAlreadyExists() throws Exception {
        // Given
        String json = objectMapper.writeValueAsString(new DatasetEditableRepresentation(DATASET_NAME, "desc"));

        createDataset();

        // When / Then
        mvc.perform(post("/api/dataset") //
                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                .andExpect(status().isConflict()) //
                .andExpect(status().reason("Dataset name=dataset_name already exists")) //
                .andDo(log());
    }

    @Test
    void createInvalid() throws Exception {
        // Given
        String json = objectMapper.writeValueAsString(new DatasetEditableRepresentation(null, "desc"));

        // When / Then
        mvc.perform(post("/api/dataset") //
                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                .andExpect(status().isBadRequest()) //
                .andDo(log());
    }

    @Test
    void deleteOK() throws Exception {
        // Given
        Integer datasetId = createDataset();

        // When / Then
        mvc.perform(delete("/api/dataset/{datasetId}", datasetId)) //
                .andExpect(status().isNoContent()) //
                .andDo(log());
    }

    @Test
    void deleteNotFound() throws Exception {
        mvc.perform(delete("/api/dataset/{datasetId}", 999)) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Dataset id=999 not found")) //
                .andDo(log());
    }

    @Test
    void findAllOK() throws Exception {
        // Given
        createDataset();

        // When / Then
        mvc.perform(get("/api/dataset?page=0&size=10")) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$._embedded.datasetEntityRepresentationList", hasSize(1))) //
                .andExpect(jsonPath("$._embedded.datasetEntityRepresentationList.[0].name", is(DATASET_NAME))) //
                .andExpect(jsonPath("$.page.size", is(10))) //
                .andExpect(jsonPath("$.page.totalElements", is(1))) //
                .andExpect(jsonPath("$._links").exists()) //
                .andDo(log());
    }

    @Test
    void findByIdOK() throws Exception {
        // Given
        Integer datasetId = createDataset();

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}", datasetId)) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").exists()) //
                .andExpect(jsonPath("$.name", is(DATASET_NAME))) //
                .andExpect(jsonPath("$.description", is(DATASET_DESC)))
                .andExpect(jsonPath("$.creation").exists()) //
                .andExpect(jsonPath("$.modification").isEmpty()) //
                .andDo(log());
    }

    @Test
    void findByIdNotFound() throws Exception {
        mvc.perform(get("/api/dataset/{datasetId}", 0)) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Dataset id=0 not found")) //
                .andDo(log());
    }

    @Test
    void findByNameOK() throws Exception {
        // Given
        createDataset();

        // When / Then
        mvc.perform(get("/api/dataset/name/{datasetName}", DATASET_NAME)) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").exists()) //
                .andExpect(jsonPath("$.name", is(DATASET_NAME))) //
                .andExpect(jsonPath("$.description", is(DATASET_DESC))) //
                .andExpect(jsonPath("$.creation").exists()) //
                .andExpect(jsonPath("$.modification").isEmpty()) //
                .andDo(log());
    }

    @Test
    void findByNameNotFound() throws Exception {
        mvc.perform(get("/api/dataset/name/{datasetName}", "none")) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Dataset name=none not found")) //
                .andDo(log());
    }

    @Test
    void updateOK() throws Exception {
        // Given
        final String DATASET_NEW_NAME = "dataset_new_name";
        String json = objectMapper.writeValueAsString(new DatasetEntityRepresentation(DATASET_NEW_NAME));

        Integer datasetId = createDataset();

        // When
        mvc.perform(put("/api/dataset/{datasetId}", datasetId) //
                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                .andExpect(status().isNoContent()) //
                .andDo(log());

        // Then
        mvc.perform(get("/api/dataset/name/{datasetName}", DATASET_NEW_NAME)) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").exists()) //
                .andExpect(jsonPath("$.name", is(DATASET_NEW_NAME))) //
                .andExpect(jsonPath("$.creation").exists()) //
                .andExpect(jsonPath("$.modification").exists()) //
                .andDo(log());
    }

    @Test
    void updateNotFound() throws Exception {
        // Given
        String json = objectMapper.writeValueAsString(new DatasetEntityRepresentation(DATASET_NAME));

        // When / Then
        mvc.perform(put("/api/dataset/{datasetId}", 0) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(json)) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Dataset id=0 not found")) //
                .andDo(log());
    }

    @Test
    void findItemsOK() throws Exception {
        // Given
        Integer datasetId = createDataset();

        addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item?page=0&size=10", datasetId)) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$._embedded.itemRepresentationList", hasSize(1))) //
                .andExpect(jsonPath("$._embedded.itemRepresentationList.[0].id").exists()) //
                .andExpect(jsonPath("$.page.size", is(10))) //
                .andExpect(jsonPath("$.page.totalElements", is(1))) //
                .andExpect(jsonPath("$._links").exists()) //
                .andDo(log());
    }

    @Test
    void findItemsDatasetEmpty() throws Exception {
        // Given
        Integer datasetId = createDataset();

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item?size=10", datasetId)) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.page.size", is(10))) //
                .andExpect(jsonPath("$.page.totalElements", is(0))) //
                .andExpect(jsonPath("$._links").exists()) //
                .andDo(log());
    }

    @Test
    void findItemOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}", datasetId, itemId)) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id", is(itemId))) //
                .andExpect(jsonPath("$.type", is("image"))) //
                .andExpect(jsonPath("$.filename", is(FILE_NAME))) //
                .andExpect(jsonPath("$.format", is("jpg"))) //
                .andExpect(jsonPath("$.hash", is(ITEM_HASH))) //
                .andExpect(jsonPath("$.creation").exists()) //
                .andExpect(jsonPath("$.modification").isEmpty()) //
                .andExpect(jsonPath("$.archiveTime", is("2005-01-17T15:20:40Z"))) //
                .andExpect(jsonPath("$.tags", hasSize(87))) //
                .andDo(log());
    }

    @Test
    void findItemDatasetNotFound() throws Exception {
        mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}", 0, 0)) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Dataset id=0 not found")) //
                .andDo(log());
    }

    @Test
    void findItemNotFound() throws Exception {
        // Given
        Integer datasetId = createDataset();

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}", datasetId, 0)) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Item id=0 not found")) //
                .andDo(log());
    }

    @Test
    void existsItemOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item/exists/{hash}", datasetId, ITEM_HASH)) //
                .andExpect(status().isOk()) //
                .andDo(log());
    }

    @Test
    void existsItemNotFound() throws Exception {
        // Given
        Integer datasetId = createDataset();

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item/exists/{hash}", datasetId, "none")) //
                .andExpect(status().isNotFound()) //
                .andExpect((status().reason("Item hash=none not found"))) //
                .andDo(log());
    }

    @Test
    void findItemContentOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}/content", datasetId, itemId)) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType("image/jpg"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + FILE_NAME + "\""))
                .andExpect(content().string(Matchers.notNullValue())) //
                .andDo(log());
    }

    @Test
    void findItemThumbnailOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}/thumbnail", datasetId, itemId)) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType("image/jpg"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"." + ITEM_HASH + "_thumb.jpg\""))
                .andExpect(content().string(Matchers.notNullValue())) //
                .andDo(log());
    }

    @Test
    void addItemOK() throws Exception {
        // Given
        Integer datasetId = createDataset();

        // When / Then
        addItem(datasetId, FILE_NAME);
    }

    @Test
    void addItemExisting() throws Exception {
        // Given
        final String ITEM_NAME = "IMG_VALID2.jpg";
        final byte[] FILE_CONTENT = getItemContent(FILE_NAME);

        Integer datasetId = createDataset();

        addItem(datasetId, ITEM_NAME);

        MockMultipartFile file = new MockMultipartFile("file", FILE_NAME, "image/jpg", FILE_CONTENT);

        // When / Then
        mvc.perform(multipart("/api/dataset/{datasetId}/item", datasetId).file(file)) //
                .andExpect(status().isConflict()) //
                .andExpect(status().reason("Item hash=" + ITEM_HASH + " already exists")) //
                .andDo(log());
    }

    @Test
    void addItemInvalid() throws Exception {
        // Given
        final String FILE_NAME_INVALID = "IMG_INVALID.jpg";
        final byte[] FILE_CONTENT = getItemContent(FILE_NAME_INVALID);

        Integer datasetId = createDataset();

        MockMultipartFile file = new MockMultipartFile("file", FILE_NAME_INVALID, "image/jpg", FILE_CONTENT);

        // When / Then
        mvc.perform(multipart("/api/dataset/{datasetId}/item", datasetId).file(file)) //
                .andExpect(status().isInternalServerError()) //
                .andExpect(status().reason("Error processing content image: File format could not be determined")) //
                .andDo(log());
    }

    @Test
    void addItemTagOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        addItemTag(datasetId, itemId);
    }

    @Test
    void addItemTagInvalid() throws Exception {
        // Given
        String json = objectMapper.writeValueAsString(new TagEntityRepresentation(TAG_ID, null, TAG_VALUE));

        Integer datasetId = createDataset();

        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(post("/api/dataset/{datasetId}/item/{itemId}/tag", datasetId, itemId) //
                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                .andExpect(status().isBadRequest()) //
                .andDo(log());
    }

    @Test
    void deleteItemOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}", datasetId, itemId)) //
                .andExpect(status().isNoContent()) //
                .andDo(log());
    }

    @Test
    void deleteItemNotFound() throws Exception {
        // Given
        Integer datasetId = createDataset();

        // When / Then
        mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}", datasetId, 0)) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Item id=0 not found")) //
                .andDo(log());
    }

    @Test
    void deleteItemTagOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);
        Integer tagId = addItemTag(datasetId, itemId);

        // When / Then
        mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}/tag/{tagId}", datasetId, itemId, tagId)) //
                .andExpect(status().isNoContent()) //
                .andDo(log());
    }

    @Test
    void deleteItemTagNotFound() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}/tag/{tagId}", datasetId, itemId, 0)) //
                .andExpect(status().isNotFound()) //
                .andExpect(status().reason("Tag id=0 not found")) //
                .andDo(log());
    }

    @Test
    void reindexOK() throws Exception {
        // Given
        Integer datasetId = createDataset();
        Integer itemId = addItem(datasetId, FILE_NAME);

        // When / Then
        mvc.perform(put("/api/dataset/{datasetId}/item/{itemId}/reindex", datasetId, itemId)) //
                .andExpect(status().isNoContent()) //
                .andDo(log());
    }

    private Integer createDataset() throws Exception {
        DatasetEntityRepresentation dataset = new DatasetEntityRepresentation(DATASET_NAME);
        dataset.setDescription(DATASET_DESC);
        String json = objectMapper.writeValueAsString(dataset);

        MvcResult result = mvc.perform(post("/api/dataset") //
                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.id").exists()) //
                .andDo(log())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private Integer addItem(Integer datasetId, String itemName) throws Exception {
        final byte[] FILE_CONTENT = getItemContent(FILE_NAME);

        MockMultipartFile file = new MockMultipartFile("file", itemName, "image/jpg", FILE_CONTENT);

        MvcResult result = mvc.perform(multipart("/api/dataset/{datasetId}/item", datasetId).file(file)) //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.id").exists()) //
                .andDo(log())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private Integer addItemTag(Integer datasetId, Integer itemId) throws Exception {
        String json = objectMapper
                .writeValueAsString(new TagEntityRepresentation(TAG_ID, TAG_NAME, TAG_VALUE));

        MvcResult result = mvc.perform(post("/api/dataset/{datasetId}/item/{itemId}/tag", datasetId, itemId) //
                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.id").exists()) //
                .andDo(log())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private byte[] getItemContent(String filename) throws IOException {
        return IOUtils.toByteArray(new ClassPathResource("images/" + filename).getInputStream());
    }
}
