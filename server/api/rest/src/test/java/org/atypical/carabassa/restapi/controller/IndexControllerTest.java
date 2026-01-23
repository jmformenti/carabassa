package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.representation.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.representation.mapper.ItemMapper;
import org.atypical.carabassa.restapi.representation.mapper.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.hateoas.Links;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = { RestApiConfiguration.class })
@ExtendWith({ RestDocumentationExtension.class })
@WebMvcTest(IndexController.class)
public class IndexControllerTest {

        @Autowired
        private MockMvc mvc;

        @MockitoBean
        private DatasetService datasetService;

        @MockitoBean
        private DatasetMapper datasetMapper;

        @MockitoBean
        private ItemMapper itemMapper;

        @MockitoBean
        private TagMapper tagMapper;

        @BeforeEach
        public void setUp(WebApplicationContext webApplicationContext,
                        RestDocumentationContextProvider restDocumentation) {
                this.mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                .apply(documentationConfiguration(restDocumentation).operationPreprocessors()
                                                .withRequestDefaults(prettyPrint()).withResponseDefaults(prettyPrint()))
                                .build();
        }

        @Test
        public void index() throws Exception {
                this.mvc.perform(get("/api/")).andExpect(status().isOk()) //
                                .andDo(document("index", //
                                                links(linkWithRel("datasets").description("Datasets resources")),
                                                responseFields(subsectionWithPath("_links")
                                                                .description("Links to other resources")
                                                                .type(Links.class))));
        }

}
