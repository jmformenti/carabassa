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
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.core.util.HashGenerator;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.IdRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;
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
import java.util.Arrays;
import java.util.List;

@Service
public class DatasetApiServiceImpl implements DatasetApiService {

    private static final int PAGE_SIZE = 100;


    @Value("${carabassa.api-url}")
    private String apiUrl;

    @Value("${carabassa.auth.token:}")
    private String token;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    private ObjectMapper objectMapper;

    @PostConstruct
    private void postConstruct() {
        String baseApiUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        WebClient.Builder builder = WebClient.builder().baseUrl(baseApiUrl);
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        } else {
            CommandLogger.warn("No token provided.");
        }
        webClient = builder.build();

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

    private boolean findItemByHash(Long datasetId, Resource item) throws IOException, ApiException {
        try {
            webClient.get()
                    .uri("dataset/{datasetId}/item/exists/{hash}", datasetId, HashGenerator.generate(item))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
            throw buildApiException(e);
        }
    }

    @Override
    public Long addItemTag(Long datasetId, Long itemId, String name, Object value, String type) throws ApiException {
        Assert.hasText(name, "Tag name can not be empty.");
        Assert.notNull(value, "Tag value can not be null.");

        TagEditableRepresentation tag = new TagEditableRepresentation(name, value, null, type);
        try {
            return webClient.post()
                    .uri("dataset/{datasetId}/item/{itemId}/tag", datasetId, itemId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(tag))
                    .retrieve()
                    .bodyToMono(IdRepresentation.class)
                    .block()
                    .getId();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
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
    public void deleteItemTag(Long datasetId, Long itemId, Long tagId) throws ApiException {
        try {
            webClient.delete()
                    .uri("dataset/{datasetId}/item/{itemId}/tag/{tagId}", datasetId, itemId, tagId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public List<DatasetEntityRepresentation> findAll() throws ApiException {
        return fetchAllPages("datasetEntityRepresentationList", DatasetEntityRepresentation.class,
                "dataset?size={size}&page={page}");
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
        return findItems(datasetId, searchString, false);
    }

    @Override
    public List<ItemRepresentation> findItems(Long datasetId, String searchString, boolean includeTags)
            throws ApiException {
        return fetchAllPages("itemRepresentationList", ItemRepresentation.class,
                "dataset/{datasetId}/item?search={searchString}&includeTags={includeTags}&size={size}&page={page}",
                datasetId, searchString, includeTags);
    }

    /**
     * Reads every page of a paged resource. The uri must end with the {size} and {page} variables.
     */
    private <T> List<T> fetchAllPages(String listName, Class<T> type, String uri, Object... uriVariables)
            throws ApiException {
        List<T> elements = new ArrayList<>();
        int page = 0;
        int totalPages;
        try {
            do {
                Object[] variables = Arrays.copyOf(uriVariables, uriVariables.length + 2);
                variables[uriVariables.length] = PAGE_SIZE;
                variables[uriVariables.length + 1] = page++;

                String json = webClient.get()
                        .uri(uri, variables)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode root = objectMapper.readTree(json);
                JsonNode list = root.path("_embedded").path(listName);
                if (list.isArray()) {
                    for (JsonNode node : list) {
                        elements.add(objectMapper.treeToValue(node, type));
                    }
                }
                totalPages = root.path("page").path("totalPages").asInt(0);
            } while (page < totalPages);
            return elements;
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
        } catch (JacksonException e) {
            throw new ApiException("Error parsing response: " + e.getMessage());
        }
    }

    @Override
    public ItemRepresentation findItem(Long datasetId, Long itemId) throws ApiException {
        try {
            return webClient.get()
                    .uri("dataset/{datasetId}/item/{itemId}", datasetId, itemId)
                    .retrieve()
                    .bodyToMono(ItemRepresentation.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw buildApiException(e);
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
