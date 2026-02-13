package org.atypical.carabassa.cli.service.impl;

import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.atypical.carabassa.cli.dto.ItemToUpload;
import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ItemAlreadyExists;
import org.atypical.carabassa.cli.exception.ResponseBodyException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.core.util.HashGenerator;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.IdRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatasetApiServiceImpl implements DatasetApiService {

    @Value("${carabassa.base-url}")
    private String baseUrl;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    private ObjectMapper objectMapper;

    @PostConstruct
    private void postConstruct() {
        String baseApiUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        webClient = webClientBuilder.baseUrl(baseApiUrl).build();

        objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Override
    public Long addItem(Long datasetId, ItemToUpload itemToUpload) throws ItemAlreadyExists, ApiException, IOException {
        Resource item = new FileSystemResource(itemToUpload.getPath());

        if (!findItemByHash(datasetId, item)) {
            MultiValueMap<String, Object> body = buildFileRequest(itemToUpload);

            try {
                return webClient.post()
                        .uri("dataset/{datasetId}/item", datasetId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(body))
                        .retrieve()
                        .bodyToMono(IdRepresentation.class)
                        .block()
                        .getId();
            } catch (WebClientResponseException e) {
                throw buildApiException(e);
            }
        } else {
            throw new ItemAlreadyExists("Item already exists.");
        }
    }

    private MultiValueMap<String, Object> buildFileRequest(ItemToUpload itemToUpload) {
        Resource item = new FileSystemResource(itemToUpload.getPath());

        // manually sets file and content type, this content type will be used in server
        // to index this file inside the dataset
        HttpHeaders fileHeaders = new HttpHeaders();
        ContentDisposition contentDisposition = ContentDisposition.builder("form-data").name("file")
                .filename(itemToUpload.getFilename()).build();
        fileHeaders.setContentDisposition(contentDisposition);
        fileHeaders.setContentType(MediaType.parseMediaType(itemToUpload.getContentType()));
        HttpEntity<Resource> entity = new HttpEntity<>(item, fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", entity);

        return body;
    }

    private boolean findItemByHash(Long datasetId, Resource item) throws IOException {
        try {
            webClient.get()
                    .uri("dataset/{datasetId}/item/exists/{hash}", datasetId, HashGenerator.generate(item))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Long create(String name, String description) throws ApiException, JacksonException {
        Assert.notNull(name, "Name can not be null.");

        DatasetEntityRepresentation dataset = new DatasetEntityRepresentation(name);
        dataset.setDescription(description);

        try {
            return webClient.post()
                    .uri("dataset")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(dataset))
                    .retrieve()
                    .bodyToMono(IdRepresentation.class)
                    .block()
                    .getId();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public void delete(Long datasetId) throws ApiException {
        try {
            webClient.delete()
                    .uri("dataset/{datasetId}", datasetId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public void deleteItem(Long datasetId, Long id) throws ApiException {
        try {
            webClient.delete()
                    .uri("dataset/{datasetId}/item/{id}", datasetId, id)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public List<DatasetEntityRepresentation> findAll() throws ApiException {
        try {
            String json = webClient.get()
                    .uri("dataset")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(json);
            JsonNode embedded = root.path("_embedded");
            JsonNode list = embedded.path("datasetEntityRepresentationList");
            List<DatasetEntityRepresentation> datasets = new ArrayList<>();
            if (list.isArray()) {
                for (JsonNode node : list) {
                    datasets.add(objectMapper.treeToValue(node, DatasetEntityRepresentation.class));
                }
            }
            return datasets;
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        } catch (JacksonException e) {
            throw new ApiException("Error parsing response: " + e.getMessage());
        }
    }

    @Override
    public Long findByName(String datasetName) throws ApiException {
        try {
            return webClient.get()
                    .uri("dataset/name/{datasetName}", datasetName)
                    .retrieve()
                    .bodyToMono(DatasetEntityRepresentation.class)
                    .block()
                    .getId();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public List<ItemRepresentation> findItems(Long datasetId) throws ApiException {
        return findItems(datasetId, null);
    }

    @Override
    public List<ItemRepresentation> findItems(Long datasetId, String searchString) throws ApiException {
        try {
            String json = webClient.get()
                    .uri("dataset/{datasetId}/item?size={size}&search={searchString}", datasetId, 100, searchString)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(json);
            JsonNode list = root.path("_embedded").path("itemRepresentationList");
            List<ItemRepresentation> items = new ArrayList<>();
            if (list.isArray()) {
                for (JsonNode node : list) {
                    items.add(objectMapper.treeToValue(node, ItemRepresentation.class));
                }
            }
            return items;
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        } catch (JacksonException e) {
            throw new ApiException("Error parsing response: " + e.getMessage());
        }
    }

    @Override
    public void reindex(Long datasetId, Long itemId) throws ApiException {
        try {
            webClient.put()
                    .uri("dataset/{datasetId}/item/{itemId}/reindex", datasetId, itemId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public void update(Long datasetId, String name, String description) throws ApiException {
        try {
            DatasetEditableRepresentation dataset = new DatasetEditableRepresentation(name, description);
            webClient.put()
                    .uri("dataset/{datasetId}", datasetId)
                    .bodyValue(dataset)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
    }

    private ApiException buildApiException(WebClientResponseException e) {
        ResponseBodyException responseBody;
        try {
            responseBody = objectMapper.readValue(e.getResponseBodyAsString(), ResponseBodyException.class);
        } catch (JacksonException je) {
            return new ApiException(e);
        }
        return new ApiException(responseBody.getMessage() + " (status code: " + e.getStatusCode().value() + ")");
    }

}
