package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.core.service.TagInfoService;
import org.atypical.carabassa.core.service.UserService;
import org.atypical.carabassa.engine.llm.LlmSearchService;
import org.atypical.carabassa.engine.llm.config.LlmProperties;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.representation.assembler.DatasetModelAssembler;
import org.atypical.carabassa.restapi.representation.assembler.ItemModelAssembler;
import org.atypical.carabassa.restapi.representation.assembler.TagInfoModelAssembler;
import org.atypical.carabassa.restapi.representation.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.representation.mapper.ItemMapper;
import org.atypical.carabassa.restapi.representation.mapper.ItemTagMapper;
import org.atypical.carabassa.restapi.representation.mapper.TagInfoMapper;
import org.atypical.carabassa.restapi.representation.mapper.TagMapper;
import org.atypical.carabassa.restapi.representation.model.AskRequestRepresentation;
import org.atypical.carabassa.restapi.test.helper.DatasetControllerHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = { RestApiConfiguration.class })
@ExtendWith({ RestDocumentationExtension.class })
@WebMvcTest(AskController.class)
public class AskControllerTest extends DatasetControllerHelper {

        private static final String QUESTION = "photos of Maria in Barcelona";
        private static final String SUMMARY = "Found 1 photo";
        private static final String SEARCH = "person:Maria location:Barcelona";

        @Autowired
        private MockMvc mvc;

        @MockitoBean
        private DatasetService datasetService;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private DatasetMapper datasetMapper;

        @MockitoBean
        private DatasetModelAssembler datasetModelAssembler;

        @MockitoBean
        private ItemModelAssembler itemModelAssembler;

        @MockitoBean
        private ItemMapper itemMapper;

        @MockitoBean
        private TagMapper tagMapper;

        @MockitoBean
        private ItemTagMapper itemTagMapper;

        @MockitoBean
        private TagInfoService tagInfoService;

        @MockitoBean
        private TagInfoMapper tagInfoMapper;

        @MockitoBean
        private TagInfoModelAssembler tagInfoModelAssembler;

        @MockitoBean
        private PagedResourcesAssembler<TagInfo> tagInfoPagedResourcesAssembler;

        @MockitoBean
        private LlmSearchService llmSearchService;

        @MockitoBean
        private LlmProperties llmProperties;

        private FieldDescriptor[] askRequestDescriptor;
        private FieldDescriptor[] askResponseDescriptor;

        @BeforeEach
        public void setUp(WebApplicationContext webApplicationContext,
                        RestDocumentationContextProvider restDocumentation) {
                this.mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                .apply(documentationConfiguration(restDocumentation).operationPreprocessors()
                                                .withRequestDefaults(prettyPrint()).withResponseDefaults(prettyPrint()))
                                .build();

                super.initData();

                this.askRequestDescriptor = new FieldDescriptor[] {
                                fieldWithPath("question").description("Natural language question") };

                this.askResponseDescriptor = new FieldDescriptor[] {
                                fieldWithPath("summary").description("LLM-generated summary"),
                                fieldWithPath("search").description("Structured search query the LLM ran")
                                                .optional(),
                                fieldWithPath("totalItems").description("Total number of matching items"),
                                subsectionWithPath("items").description("First page of matching items") };
        }

        @Test
        public void ask() throws Exception {
                String json = objectMapper.writeValueAsString(askRequest(QUESTION));

                Page<IndexedItem> page = new PageImpl<>(List.of(indexedItem), PageRequest.of(0, 10), 1L);
                LlmSearchService.AskResult result = new LlmSearchService.AskResult(SUMMARY, SEARCH, page);

                when(llmProperties.isEnabled()).thenReturn(true);
                when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
                when(llmSearchService.ask(eq(dataset), eq(QUESTION))).thenReturn(result);
                when(itemModelAssembler.toDetailedModel(isA(IndexedItem.class))).thenReturn(itemRepresentation);

                mvc.perform(post("/api/dataset/{datasetId}/ask", DATASET_ID) //
                                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                                .andExpect(status().isOk()) //
                                .andExpect(jsonPath("$.summary").value(SUMMARY)) //
                                .andExpect(jsonPath("$.search").value(SEARCH)) //
                                .andExpect(jsonPath("$.totalItems").value(1)) //
                                .andDo(document("ask", //
                                                pathParameters(parameterWithName("datasetId")
                                                                .description("Dataset identifier")),
                                                requestFields(askRequestDescriptor),
                                                responseFields(askResponseDescriptor)));
        }

        @Test
        public void askWithoutSearchReturnsEmptyItems() throws Exception {
                String json = objectMapper.writeValueAsString(askRequest(QUESTION));

                LlmSearchService.AskResult result = new LlmSearchService.AskResult(SUMMARY, null, null);

                when(llmProperties.isEnabled()).thenReturn(true);
                when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
                when(llmSearchService.ask(eq(dataset), eq(QUESTION))).thenReturn(result);

                mvc.perform(post("/api/dataset/{datasetId}/ask", DATASET_ID) //
                                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                                .andExpect(status().isOk()) //
                                .andExpect(jsonPath("$.summary").value(SUMMARY)) //
                                .andExpect(jsonPath("$.totalItems").value(0)) //
                                .andExpect(jsonPath("$.items").isEmpty());
        }

        @Test
        public void askWhenLlmDisabledReturnsServiceUnavailable() throws Exception {
                String json = objectMapper.writeValueAsString(askRequest(QUESTION));

                when(llmProperties.isEnabled()).thenReturn(false);

                mvc.perform(post("/api/dataset/{datasetId}/ask", DATASET_ID) //
                                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                                .andExpect(status().isServiceUnavailable());
        }

        @Test
        public void askWhenDatasetNotFoundReturnsNotFound() throws Exception {
                String json = objectMapper.writeValueAsString(askRequest(QUESTION));

                when(llmProperties.isEnabled()).thenReturn(true);
                when(datasetService.findById(DATASET_ID))
                                .thenThrow(new EntityNotFoundException("dataset not found"));

                mvc.perform(post("/api/dataset/{datasetId}/ask", DATASET_ID) //
                                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                                .andExpect(status().isNotFound());
        }

        @Test
        public void askWhenLlmFailsReturnsBadGateway() throws Exception {
                String json = objectMapper.writeValueAsString(askRequest(QUESTION));

                when(llmProperties.isEnabled()).thenReturn(true);
                when(datasetService.findById(DATASET_ID)).thenReturn(dataset);
                when(llmSearchService.ask(eq(dataset), eq(QUESTION)))
                                .thenThrow(new IllegalStateException("LLM invocation failed"));

                mvc.perform(post("/api/dataset/{datasetId}/ask", DATASET_ID) //
                                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                                .andExpect(status().isBadGateway());
        }

        @Test
        public void askWithBlankQuestionReturnsBadRequest() throws Exception {
                String json = objectMapper.writeValueAsString(askRequest(""));

                mvc.perform(post("/api/dataset/{datasetId}/ask", DATASET_ID) //
                                .contentType(MediaType.APPLICATION_JSON).content(json)) //
                                .andExpect(status().isBadRequest());
        }

        private AskRequestRepresentation askRequest(String question) {
                AskRequestRepresentation request = new AskRequestRepresentation();
                request.setQuestion(question);
                return request;
        }
}
