package org.atypical.carabassa.restapi.controller;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.impl.DatasetImpl;
import org.atypical.carabassa.core.model.impl.StoredItemImpl;
import org.atypical.carabassa.core.model.impl.StoredItemInfoImpl;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.representation.assembler.DatasetModelAssembler;
import org.atypical.carabassa.restapi.representation.assembler.ItemModelAssembler;
import org.atypical.carabassa.restapi.representation.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.representation.mapper.ItemMapper;
import org.atypical.carabassa.restapi.representation.mapper.TagMapper;
import org.atypical.carabassa.restapi.representation.model.BoundingBoxRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;
import org.atypical.carabassa.restapi.test.helper.DatasetControllerHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Links;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ContextConfiguration(classes = { RestApiConfiguration.class })
@ExtendWith({ RestDocumentationExtension.class })
@WebMvcTest(DatasetController.class)
public class DatasetControllerTest extends DatasetControllerHelper {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private DatasetService datasetService;

	@MockBean
	private DatasetMapper datasetMapper;

	@MockBean
	private DatasetModelAssembler datasetModelAssembler;

	@MockBean
	private ItemModelAssembler itemModelAssembler;

	@MockBean
	private ItemMapper itemMapper;

	@MockBean
	private TagMapper tagMapper;

	private FieldDescriptor[] datasetDescriptor;
	private FieldDescriptor[] datasetEditableDescriptor;
	private FieldDescriptor[] itemDescriptor;
	private FieldDescriptor[] tagEditableDescriptor;
	private LinksSnippet pagingLinks;

	@BeforeEach
	public void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
		this.mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(documentationConfiguration(restDocumentation).operationPreprocessors()
						.withRequestDefaults(prettyPrint()).withResponseDefaults(prettyPrint()))
				.build();

		super.initData();

		this.datasetDescriptor = getDatasetDescriptor();
		this.datasetEditableDescriptor = getDatasetEditableDescriptor();
		this.itemDescriptor = getItemDescriptor();
		this.tagEditableDescriptor = getTagEditableDescriptor();
		this.pagingLinks = getPageLinks();
	}

	private FieldDescriptor[] getDatasetEditableDescriptor() {
		return new FieldDescriptor[] { fieldWithPath("name").description("Dataset name"),
				fieldWithPath("description").description("Dataset description") };
	}

	private FieldDescriptor[] getDatasetDescriptor() {
		return new FieldDescriptor[] { fieldWithPath("id").description("Dataset identifier"),
				fieldWithPath("name").description("Dataset name"),
				fieldWithPath("description").description("Dataset description"),
				fieldWithPath("creation").description("Dataset creation date"),
				fieldWithPath("modification").description("Dataset last modification date") };
	}

	private FieldDescriptor[] getItemDescriptor() {
		return new FieldDescriptor[] { fieldWithPath("id").description("Item identifier"),
				fieldWithPath("type").description("Item type"), fieldWithPath("format").description("Item format"),
				fieldWithPath("filename").description("Item original filename"),
				fieldWithPath("hash").description("Item unique hash"),
				fieldWithPath("creation").description("Item date creation in repository"),
				fieldWithPath("modification").description("Item date last modification in repository"),
				fieldWithPath("archiveTime").description("Item archived date (by default, shot date)"),
				subsectionWithPath("tags[]").description("Array of tags") };
	}

	private FieldDescriptor[] getTagEditableDescriptor() {
		return new FieldDescriptor[] { fieldWithPath("name").description("Tag name"),
				fieldWithPath("value").description("Tag value"), subsectionWithPath("boundingBox")
						.description("Bounding box related").type(BoundingBoxRepresentation.class) };
	}

	private LinksSnippet getPageLinks() {
		return links(linkWithRel(IanaLinkRelations.SELF.toString()).description("Current page"),
				linkWithRel("first").optional().description("The first page of results"),
				linkWithRel("last").optional().description("The last page of results"),
				linkWithRel("next").optional().description("The next page of results"),
				linkWithRel("prev").optional().description("The previous page of results"));
	}

	@Test
	public void create() throws Exception {
		String json = objectMapper.writeValueAsString(new DatasetEditableRepresentation(DATASET_NAME, "description"));

		when(datasetMapper.toEntity(isA(DatasetEditableRepresentation.class))).thenReturn(dataset);
		when(datasetService.create(dataset)).thenReturn(dataset);

		mvc.perform(post("/api/dataset") //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isCreated()) //
				.andDo(document("create", //
						requestFields(datasetEditableDescriptor),
						responseFields(fieldWithPath("id").description("New dataset identifier"))));
	}

	@Test
	public void findAll() throws Exception {
		Page<Dataset> page = new PageImpl<>(Arrays.asList(new DatasetImpl()), PageRequest.of(2, 10), 100L);

		when(datasetService.findAll(isA(Pageable.class))).thenReturn(page);
		when(datasetModelAssembler.toModel(isA(Dataset.class))).thenReturn(datasetRepresentation);

		this.mvc.perform(get("/api/dataset?page=2&size=10")) //
				.andExpect(status().isOk()) //
				.andDo(document("find-all", //
						requestParameters(parameterWithName("page").description("The page to retrieve"),
								parameterWithName("size").description("Entries per page")),
						pagingLinks, //
						responseFields(
								subsectionWithPath("_links").description("Links to other resources").type(Links.class),
								subsectionWithPath("_embedded.datasetEntityRepresentationList[]")
										.description("List of datasets"),
								subsectionWithPath("page").description("Page metadata").type(Page.class))));
	}

	@Test
	public void findById() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetMapper.toRepresentation(isA(Dataset.class))).thenReturn(datasetRepresentation);

		mvc.perform(get("/api/dataset/{datasetId}", DATASET_ID)) //
				.andExpect(status().isOk()) //
				.andDo(document("find-by-id", //
						pathParameters(parameterWithName("datasetId").description("Dataset identifier")),
						responseFields(datasetDescriptor)));
	}

	@Test
	public void findByName() throws Exception {
		when(datasetService.findByName(DATASET_NAME)).thenReturn(dataset);
		when(datasetMapper.toRepresentation(isA(Dataset.class))).thenReturn(datasetRepresentation);

		mvc.perform(get("/api/dataset/name/{datasetName}", DATASET_NAME)) //
				.andExpect(status().isOk()) //
				.andDo(document("find-by-name", //
						pathParameters(parameterWithName("datasetName").description("Dataset name")),
						responseFields(datasetDescriptor)));
	}

	@Test
	public void update() throws Exception {
		String json = objectMapper.writeValueAsString(new DatasetEditableRepresentation(DATASET_NAME, "description"));

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.update(dataset)).thenReturn(dataset);

		mvc.perform(put("/api/dataset/{datasetId}", DATASET_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isNoContent()) //
				.andDo(document("update", //
						pathParameters(parameterWithName("datasetId").description("Dataset identifier")),
						requestFields(datasetEditableDescriptor)));
	}

	@Test
	public void deleteOK() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		doNothing().when(datasetService).delete(dataset);

		mvc.perform(delete("/api/dataset/{datasetId}", DATASET_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(document("delete", //
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"))));
	}

	@Test
	public void getItems() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		Page<IndexedItem> page = new PageImpl<>(new ArrayList<>(dataset.getItems()), PageRequest.of(2, 10), 100L);
		when(datasetService.findItems(isA(Dataset.class), isA(Pageable.class))).thenReturn(page);
		when(itemModelAssembler.toModel(isA(IndexedItem.class))).thenReturn(itemRepresentation);

		mvc.perform(get("/api/dataset/{datasetId}/item?page=0&size=10", DATASET_ID)) //
				.andExpect(status().isOk()) //
				.andDo(document("get-items", //
						pathParameters(parameterWithName("datasetId").description("Dataset identifier")),
						requestParameters(parameterWithName("page").description("The page to retrieve"),
								parameterWithName("size").description("Entries per page")),
						pagingLinks, //
						responseFields(
								subsectionWithPath("_links").description("Links to other resources").type(Links.class),
								subsectionWithPath("_embedded.itemRepresentationList[]").description("Array of items"),
								subsectionWithPath("page").description("Page metadata").type(Page.class))));
	}

	@Test
	public void getItem() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		when(itemMapper.toRepresentation(indexedItem)).thenReturn(itemRepresentation);

		mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isOk()) //
				.andDo(document("get-item",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("itemId").description("Item identifier")),
						responseFields(itemDescriptor)));
	}

	@Test
	void existsItem() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemByHash(dataset, ITEM_HASH)).thenReturn(indexedItem);

		mvc.perform(get("/api/dataset/{datasetId}/item/exists/{hash}", DATASET_ID, ITEM_HASH)) //
				.andExpect(status().isOk()) //
				.andDo(document("exists-item",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("hash").description("Item hash (hexadecimal md5 digest)"))));
	}

	@Test
	public void getItemContent() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);

		StoredItem storedItem = new StoredItemImpl();
		Resource resource = new ClassPathResource("images/IMG_NO_DATE.jpg");
		storedItem.setStoredItemInfo(new StoredItemInfoImpl(resource.getFilename()));
		byte[] sampleContent = new byte[1000];
		resource.getInputStream().read(sampleContent);
		storedItem.setContent(sampleContent);
		when(datasetService.getStoredItem(dataset, indexedItem)).thenReturn(storedItem);

		mvc.perform(get("/api/dataset/{datasetId}/item/{itemId}/content", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isOk()) //
				.andDo(document("get-item-content",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("itemId").description("Item identifier"))));
	}

	@Test
	public void addItem() throws Exception {
		Resource resource = new ClassPathResource("images/IMG_NO_DATE.jpg");
		byte[] sampleContent = new byte[1000];
		resource.getInputStream().read(sampleContent);

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addItem(isA(Dataset.class), isA(ItemType.class), isA(String.class), isA(Resource.class)))
				.thenReturn(indexedItem);

		MockMultipartFile file = new MockMultipartFile("file", indexedItem.getFilename(), "image/jpg", sampleContent);

		mvc.perform(fileUpload("/api/dataset/{datasetId}/item", DATASET_ID).file(file)) //
				.andExpect(status().isCreated()) //
				.andDo(document("add-item",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier")),
						responseFields(fieldWithPath("id").description("New item identifier"))));
	}

	@Test
	public void addItemTag() throws Exception {
		TagEditableRepresentation tagEditableRepresentation = new TagEditableRepresentation(TAG_NAME, TAG_VALUE,
				new BoundingBoxRepresentation(10, 20, 30, 40));
		String json = objectMapper.writeValueAsString(tagEditableRepresentation);

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(tagMapper.toEntity(isA(TagEditableRepresentation.class))).thenReturn(tag);
		when(datasetService.addItemTag(isA(Dataset.class), isA(Long.class), isA(Tag.class))).thenReturn(TAG_ID);

		mvc.perform(post("/api/dataset/{datasetId}/item/{itemId}/tag", DATASET_ID, ITEM_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isCreated()) //
				.andDo(document("add-item-tag",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("itemId").description("Item identifier")),
						requestFields(tagEditableDescriptor),
						responseFields(fieldWithPath("id").description("New tag identifier"))));
	}

	@Test
	public void deleteItem() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		doNothing().when(datasetService).deleteItem(dataset, ITEM_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}", DATASET_ID, ITEM_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(document("delete-item",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("itemId").description("Item identifier"))));
	}

	@Test
	public void deleteItemTag() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findItemById(dataset, ITEM_ID)).thenReturn(indexedItem);
		doNothing().when(datasetService).deleteItemTag(dataset, ITEM_ID, TAG_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/item/{itemId}/tag/{tagId}", DATASET_ID, ITEM_ID, TAG_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(document("delete-item-tag",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("itemId").description("Item identifier"),
								parameterWithName("tagId").description("Tag identifier"))));
	}

}
