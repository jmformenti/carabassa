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
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.impl.DatasetImpl;
import org.atypical.carabassa.core.model.impl.StoredImageImpl;
import org.atypical.carabassa.core.model.impl.StoredImageInfoImpl;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.mapper.ImageMapper;
import org.atypical.carabassa.restapi.mapper.TagMapper;
import org.atypical.carabassa.restapi.representation.assembler.DatasetModelAssembler;
import org.atypical.carabassa.restapi.representation.assembler.ImageModelAssembler;
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
	private ImageModelAssembler imageModelAssembler;

	@MockBean
	private ImageMapper imageMapper;

	@MockBean
	private TagMapper tagMapper;

	private FieldDescriptor[] datasetDescriptor;
	private FieldDescriptor[] datasetEditableDescriptor;
	private FieldDescriptor[] imageDescriptor;
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
		this.imageDescriptor = getImageDescriptor();
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

	private FieldDescriptor[] getImageDescriptor() {
		return new FieldDescriptor[] { fieldWithPath("id").description("Image identifier"),
				fieldWithPath("filename").description("Image original filename"),
				fieldWithPath("fileType").description("Image type"),
				fieldWithPath("hash").description("Image unique hash"),
				fieldWithPath("creation").description("Image date creation in repository"),
				fieldWithPath("modification").description("Image date last modification in repository"),
				fieldWithPath("archiveTime").description("Image archived date (by default, shot date)"),
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
	public void getImages() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		Page<IndexedImage> page = new PageImpl<>(new ArrayList<>(dataset.getImages()), PageRequest.of(2, 10), 100L);
		when(datasetService.findImages(isA(Dataset.class), isA(Pageable.class))).thenReturn(page);
		when(imageModelAssembler.toModel(isA(IndexedImage.class))).thenReturn(imageRepresentation);

		mvc.perform(get("/api/dataset/{datasetId}/image?page=0&size=10", DATASET_ID)) //
				.andExpect(status().isOk()) //
				.andDo(document("get-images", //
						pathParameters(parameterWithName("datasetId").description("Dataset identifier")),
						requestParameters(parameterWithName("page").description("The page to retrieve"),
								parameterWithName("size").description("Entries per page")),
						pagingLinks, //
						responseFields(
								subsectionWithPath("_links").description("Links to other resources").type(Links.class),
								subsectionWithPath("_embedded.imageRepresentationList[]")
										.description("Array of images"),
								subsectionWithPath("page").description("Page metadata").type(Page.class))));
	}

	@Test
	public void getImage() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		when(imageMapper.toRepresentation(indexedImage)).thenReturn(imageRepresentation);

		mvc.perform(get("/api/dataset/{datasetId}/image/{imageId}", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isOk()) //
				.andDo(document("get-image",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("imageId").description("Image identifier")),
						responseFields(imageDescriptor)));
	}

	@Test
	public void getImageContent() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);

		StoredImage storedImage = new StoredImageImpl();
		Resource resource = new ClassPathResource("images/IMG_NO_DATE.jpg");
		storedImage.setStoredImageInfo(new StoredImageInfoImpl(resource.getFilename()));
		byte[] sampleContent = new byte[1000];
		resource.getInputStream().read(sampleContent);
		storedImage.setContent(sampleContent);
		when(datasetService.getStoredImage(dataset, indexedImage)).thenReturn(storedImage);

		mvc.perform(get("/api/dataset/{datasetId}/image/{imageId}/content", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isOk()) //
				.andDo(document("get-image-content",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("imageId").description("Image identifier"))));
	}

	@Test
	public void addImage() throws Exception {
		Resource resource = new ClassPathResource("images/IMG_NO_DATE.jpg");
		byte[] sampleContent = new byte[1000];
		resource.getInputStream().read(sampleContent);

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.addImage(isA(Dataset.class), isA(Resource.class))).thenReturn(indexedImage);

		MockMultipartFile file = new MockMultipartFile("file", indexedImage.getFilename(), null, sampleContent);

		mvc.perform(fileUpload("/api/dataset/{datasetId}/image", DATASET_ID).file(file)) //
				.andExpect(status().isCreated()) //
				.andDo(document("add-image",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier")),
						responseFields(fieldWithPath("id").description("New image identifier"))));
	}

	@Test
	public void addImageTag() throws Exception {
		TagEditableRepresentation tagEditableRepresentation = new TagEditableRepresentation(TAG_NAME, TAG_VALUE,
				new BoundingBoxRepresentation(10, 20, 30, 40));
		String json = objectMapper.writeValueAsString(tagEditableRepresentation);

		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(tagMapper.toEntity(isA(TagEditableRepresentation.class))).thenReturn(tag);
		when(datasetService.addImageTag(isA(Dataset.class), isA(Long.class), isA(Tag.class))).thenReturn(TAG_ID);

		mvc.perform(post("/api/dataset/{datasetId}/image/{imageId}/tag", DATASET_ID, IMAGE_ID) //
				.contentType(MediaType.APPLICATION_JSON).content(json)) //
				.andExpect(status().isCreated()) //
				.andDo(document("add-image-tag",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("imageId").description("Image identifier")),
						requestFields(tagEditableDescriptor),
						responseFields(fieldWithPath("id").description("New tag identifier"))));
	}

	@Test
	public void deleteImage() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		doNothing().when(datasetService).deleteImage(dataset, IMAGE_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/image/{imageId}", DATASET_ID, IMAGE_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(document("delete-image",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("imageId").description("Image identifier"))));
	}

	@Test
	public void deleteImageTag() throws Exception {
		when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
		when(datasetService.findImageById(dataset, IMAGE_ID)).thenReturn(indexedImage);
		doNothing().when(datasetService).deleteImageTag(dataset, IMAGE_ID, TAG_ID);

		mvc.perform(delete("/api/dataset/{datasetId}/image/{imageId}/tag/{tagId}", DATASET_ID, IMAGE_ID, TAG_ID)) //
				.andExpect(status().isNoContent()) //
				.andDo(document("delete-image-tag",
						pathParameters(parameterWithName("datasetId").description("Dataset identifier"),
								parameterWithName("imageId").description("Image identifier"),
								parameterWithName("tagId").description("Tag identifier"))));
	}

}
