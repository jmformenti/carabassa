package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.core.service.TagInfoService;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.representation.assembler.DatasetModelAssembler;
import org.atypical.carabassa.restapi.representation.assembler.ItemModelAssembler;
import org.atypical.carabassa.restapi.representation.assembler.TagInfoModelAssembler;
import org.atypical.carabassa.restapi.representation.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.representation.mapper.ItemMapper;
import org.atypical.carabassa.restapi.representation.mapper.ItemTagMapper;
import org.atypical.carabassa.restapi.representation.mapper.TagMapper;
import org.atypical.carabassa.restapi.representation.mapper.TagInfoMapper;
import org.atypical.carabassa.restapi.representation.model.TagInfoEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagInfoEntityRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = { RestApiConfiguration.class })
@ExtendWith({ RestDocumentationExtension.class })
@WebMvcTest(TagInfoController.class)
public class TagInfoControllerTest {

    private static final Long TAG_INFO_ID = 1L;
    private static final String TAG_NAME = "tag.name";
    private static final String TAG_ALIAS = "alias";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TagInfoService tagInfoService;

    @MockitoBean
    private TagInfoMapper tagInfoMapper;

    @MockitoBean
    private TagInfoModelAssembler tagInfoModelAssembler;

    @MockitoBean
    private PagedResourcesAssembler<TagInfo> tagInfoPagedResourcesAssembler;

    @MockitoBean
    private DatasetService datasetService;

    @MockitoBean
    private DatasetMapper datasetMapper;

    @MockitoBean
    private ItemMapper itemMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private ItemTagMapper itemTagMapper;

    @MockitoBean
    private DatasetModelAssembler datasetModelAssembler;

    @MockitoBean
    private ItemModelAssembler itemModelAssembler;

    @MockitoBean
    private PagedResourcesAssembler<Dataset> datasetPagedResourcesAssembler;

    @MockitoBean
    private PagedResourcesAssembler<IndexedItem> itemPagedResourcesAssembler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TagInfo tagInfo;
    private TagInfoEditableRepresentation tagInfoEditable;
    private TagInfoEntityRepresentation tagInfoRepresentation;
    private FieldDescriptor[] tagInfoDescriptor;
    private FieldDescriptor[] tagInfoEditableDescriptor;
    private LinksSnippet pagingLinks;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext,
                      RestDocumentationContextProvider restDocumentation) {
        this.mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation).operationPreprocessors()
                        .withRequestDefaults(prettyPrint()).withResponseDefaults(prettyPrint()))
                .build();

        tagInfo = org.mockito.Mockito.mock(TagInfo.class);
        when(tagInfo.getId()).thenReturn(TAG_INFO_ID);
        when(tagInfo.getTagName()).thenReturn(TAG_NAME);
        when(tagInfo.getAlias()).thenReturn(TAG_ALIAS);
        when(tagInfo.getDescription()).thenReturn("description");
        when(tagInfo.getInternal()).thenReturn(false);

        tagInfoEditable = new TagInfoEditableRepresentation(TAG_NAME, "description", TAG_ALIAS, false);

        tagInfoRepresentation = new TagInfoEntityRepresentation();
        tagInfoRepresentation.setId(TAG_INFO_ID);
        tagInfoRepresentation.setTagName(TAG_NAME);
        tagInfoRepresentation.setDescription("description");
        tagInfoRepresentation.setAlias(TAG_ALIAS);
        tagInfoRepresentation.setInternal(false);

        tagInfoDescriptor = getTagInfoDescriptor();
        tagInfoEditableDescriptor = getTagInfoEditableDescriptor();
        pagingLinks = getPageLinks();
    }

    private FieldDescriptor[] getTagInfoDescriptor() {
        return new FieldDescriptor[] {
                fieldWithPath("id").description("Tag info identifier"),
                fieldWithPath("tagName").description("Tag name"),
                fieldWithPath("description").description("Tag description"),
                fieldWithPath("alias").description("Tag alias"),
                fieldWithPath("internal").description("Internal tag flag")
        };
    }

    private FieldDescriptor[] getTagInfoEditableDescriptor() {
        return new FieldDescriptor[] {
                fieldWithPath("tagName").description("Tag name"),
                fieldWithPath("description").description("Tag description"),
                fieldWithPath("alias").description("Tag alias"),
                fieldWithPath("internal").description("Internal tag flag")
        };
    }

    private LinksSnippet getPageLinks() {
        return links(linkWithRel(IanaLinkRelations.SELF.toString()).description("Current page"),
                linkWithRel("first").optional().description("The first page of results"),
                linkWithRel("last").optional().description("The last page of results"),
                linkWithRel("next").optional().description("The next page of results"),
                linkWithRel("prev").optional().description("The previous page of results"));
    }

    @Test
    public void createTagInfo() throws Exception {
        String json = objectMapper.writeValueAsString(tagInfoEditable);

        when(tagInfoMapper.toEntity(isA(TagInfoEditableRepresentation.class))).thenReturn(tagInfo);
        when(tagInfoService.create(tagInfo)).thenReturn(tagInfo);

        mvc.perform(post("/api/tag-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TAG_INFO_ID))
                .andDo(document("tag-info-create",
                        requestFields(tagInfoEditableDescriptor),
                        responseFields(fieldWithPath("id").description("New tag info identifier"))));
    }

    @Test
    public void findAllTagInfo() throws Exception {
        Page<TagInfo> page = new PageImpl<>(List.of(tagInfo), PageRequest.of(0, 20), 1L);
        PagedModel<TagInfoEntityRepresentation> pagedModel = PagedModel.of(
                List.of(tagInfoRepresentation),
                new PagedModel.PageMetadata(1, 0, 1),
                org.springframework.hateoas.Link.of("/api/tag-info?page=0&size=20")
                        .withRel(IanaLinkRelations.SELF)
        );

        when(tagInfoService.findAll(isA(Pageable.class))).thenReturn(page);
        when(tagInfoPagedResourcesAssembler.toModel(isA(Page.class), isA(TagInfoModelAssembler.class)))
                .thenReturn(pagedModel);

        mvc.perform(get("/api/tag-info?page=0&size=20"))
                .andExpect(status().isOk())
                .andDo(document("tag-info-find-all",
                        queryParameters(
                                parameterWithName("page").description("The page to retrieve"),
                                parameterWithName("size").description("Entries per page")),
                        pagingLinks,
                        responseFields(
                                subsectionWithPath("page").description("Pagination details"),
                                subsectionWithPath("_links").description("Links to other resources").type(Links.class),
                                subsectionWithPath("_embedded.tagInfoEntityRepresentationList[]")
                                        .description("Tag info list"))
                ));
    }

    @Test
    public void findTagInfoById() throws Exception {
        when(tagInfoService.findById(TAG_INFO_ID)).thenReturn(tagInfo);
        when(tagInfoModelAssembler.toModel(tagInfo)).thenReturn(tagInfoRepresentation);

        mvc.perform(get("/api/tag-info/{id}", TAG_INFO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TAG_INFO_ID))
                .andExpect(jsonPath("$.tagName").value(TAG_NAME))
                .andDo(document("tag-info-find-by-id",
                        pathParameters(parameterWithName("id").description("Tag info id")),
                        responseFields(tagInfoDescriptor)));
    }

    @Test
    public void updateTagInfo() throws Exception {
        String json = objectMapper.writeValueAsString(tagInfoEditable);

        when(tagInfoMapper.toEntity(isA(TagInfoEditableRepresentation.class))).thenReturn(tagInfo);
        when(tagInfoService.update(TAG_INFO_ID, tagInfo)).thenReturn(tagInfo);

        mvc.perform(put("/api/tag-info/{id}", TAG_INFO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent())
                .andDo(document("tag-info-update",
                        pathParameters(parameterWithName("id").description("Tag info id")),
                        requestFields(tagInfoEditableDescriptor)));
    }

    @Test
    public void deleteTagInfo() throws Exception {
        doNothing().when(tagInfoService).delete(TAG_INFO_ID);

        mvc.perform(delete("/api/tag-info/{id}", TAG_INFO_ID))
                .andExpect(status().isNoContent())
                .andDo(document("tag-info-delete",
                        pathParameters(parameterWithName("id").description("Tag info id"))));
    }
}
