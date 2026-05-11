package org.atypical.carabassa.restapi.controller.impl;

import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.engine.llm.LlmSearchService;
import org.atypical.carabassa.engine.llm.config.LlmProperties;
import org.atypical.carabassa.restapi.controller.AskController;
import org.atypical.carabassa.restapi.representation.assembler.ItemModelAssembler;
import org.atypical.carabassa.restapi.representation.model.AskRequestRepresentation;
import org.atypical.carabassa.restapi.representation.model.AskResponseRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AskControllerImpl implements AskController {

    private static final Logger logger = LoggerFactory.getLogger(AskControllerImpl.class);

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private ItemModelAssembler itemModelAssembler;

    @Autowired
    private LlmSearchService llmSearchService;

    @Override
    public AskResponseRepresentation ask(Long datasetId, AskRequestRepresentation request) {
        if (!llmProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Natural language search is not configured. Set CARABASSA_LLM_URL and CARABASSA_LLM_MODEL on the backend to enable it.");
        }

        Dataset dataset = getDataset(datasetId);
        LlmSearchService.AskResult result;
        try {
            result = llmSearchService.ask(dataset, request.getQuestion());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("LLM ask failed for dataset {}", datasetId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Natural language search failed. Please try again later.");
        }

        Page<IndexedItem> page = result.page();
        List<ItemRepresentation> items = page == null
                ? Collections.emptyList()
                : page.getContent().stream()
                    .map(itemModelAssembler::toDetailedModel)
                    .collect(Collectors.toList());
        long total = page == null ? 0 : page.getTotalElements();
        return new AskResponseRepresentation(result.summary(), result.search(), total, items);
    }

    private Dataset getDataset(Long datasetId) {
        try {
            return datasetService.findById(datasetId);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
