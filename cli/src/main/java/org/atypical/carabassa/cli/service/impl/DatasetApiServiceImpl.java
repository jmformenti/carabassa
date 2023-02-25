package org.atypical.carabassa.cli.service.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.TypeReferences.PagedModelType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class DatasetApiServiceImpl implements DatasetApiService {

    private final static String DATASET_PATH = "dataset/";

    @Value("${carabassa.base-url}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    private String datasetUrl;

    @PostConstruct
    private void postConstruct() {
        datasetUrl = getDatasetUrl();

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Long addItem(Long datasetId, ItemToUpload itemToUpload) throws ItemAlreadyExists, ApiException, IOException {
        Resource item = new FileSystemResource(itemToUpload.getPath());

        if (!findItemByHash(datasetId, item)) {
            HttpEntity<MultiValueMap<String, Object>> request = buildFileRequest(itemToUpload);

            ResponseEntity<IdRepresentation> response;
            try {
                response = restTemplate.exchange(datasetUrl + "{datasetId}/item", HttpMethod.POST, request,
                        new ParameterizedTypeReference<>() {
                        }, datasetId);
            } catch (RestClientResponseException e) {
                throw buildApiException(e);
            }

            return response.getBody().getId();
        } else {
            throw new ItemAlreadyExists("Item already exists.");
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> buildFileRequest(ItemToUpload itemToUpload) {
        Resource item = new FileSystemResource(itemToUpload.getPath());

        // multi from data request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // no buffer to avoid OOM in large files
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        restTemplate.setRequestFactory(requestFactory);

        // manually sets file and content type, this content type will be used in server
        // to index this file inside the dataset
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition.builder("form-data").name("file")
                .filename(itemToUpload.getFilename()).build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        fileMap.add(HttpHeaders.CONTENT_TYPE, itemToUpload.getContentType());
        HttpEntity<Resource> entity = new HttpEntity<>(item, fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", entity);

        return new HttpEntity<>(body, headers);
    }

    private boolean findItemByHash(Long datasetId, Resource item) throws IOException {
        try {
            restTemplate.getForEntity(datasetUrl + "{datasetId}/item/exists/{hash}", Void.class, datasetId,
                    HashGenerator.generate(item));
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Long create(String name, String description) throws ApiException, JsonProcessingException {
        Assert.notNull(name, "Name can not be null.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        DatasetEntityRepresentation dataset = new DatasetEntityRepresentation(name);
        dataset.setDescription(description);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(dataset), headers);

        ResponseEntity<IdRepresentation> response;
        try {
            response = restTemplate.exchange(datasetUrl, HttpMethod.POST, request,
                    new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
        return response.getBody().getId();
    }

    @Override
    public void delete(Long datasetId) throws ApiException {
        try {
            restTemplate.delete(datasetUrl + "{datasetId}", datasetId);
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public void deleteItem(Long datasetId, Long id) throws ApiException {
        try {
            restTemplate.delete(datasetUrl + "/{datasetId}/item/{id}", datasetId, id);
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public List<DatasetEntityRepresentation> findAll() throws ApiException {
        try {
            PagedModel<DatasetEntityRepresentation> page = getPage(datasetUrl,
                    new PagedModelType<DatasetEntityRepresentation>() {
                    });

            List<DatasetEntityRepresentation> datasets = new ArrayList<>(page.getContent());
            while (page.hasLink(IanaLinkRelations.NEXT)) {
                page = getPage(page.getLink(IanaLinkRelations.NEXT_VALUE).get().toUri(),
                        new PagedModelType<DatasetEntityRepresentation>() {
                        });
                datasets.addAll(page.getContent());
            }

            return datasets;
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public Long findByName(String datasetName) throws ApiException {
        ResponseEntity<DatasetEntityRepresentation> response;
        try {
            response = restTemplate.getForEntity(datasetUrl + "name/{datasetName}", DatasetEntityRepresentation.class,
                    datasetName);
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
        return response.getBody().getId();
    }

    @Override
    public List<ItemRepresentation> findItems(Long datasetId) throws ApiException {
        return findItems(datasetId, null);
    }

    @Override
    public List<ItemRepresentation> findItems(Long datasetId, String searchString) throws ApiException {
        try {
            PagedModel<ItemRepresentation> page = getPage(
                    datasetUrl + "{datasetId}/item?size={size}&search={searchString}",
                    new PagedModelType<ItemRepresentation>() {
                    }, datasetId, 100, searchString);

            List<ItemRepresentation> items = new ArrayList<>(page.getContent());
            while (page.hasLink(IanaLinkRelations.NEXT)) {
                page = getPage(page.getLink(IanaLinkRelations.NEXT_VALUE).get().toUri(),
                        new PagedModelType<ItemRepresentation>() {
                        });
                items.addAll(page.getContent());
            }

            return items;
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public void resetItem(Long datasetId, Long itemId) throws ApiException {
        try {
            restTemplate.put(datasetUrl + "{datasetId}/item/{itemId}/reset", null, datasetId, itemId);
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
    }

    @Override
    public void update(Long datasetId, String description) throws ApiException {
        try {
            DatasetEntityRepresentation dataset = new DatasetEntityRepresentation();
            dataset.setDescription(description);
            restTemplate.put(datasetUrl + "{datasetId}", dataset, datasetId);
        } catch (RestClientResponseException e) {
            throw buildApiException(e);
        }
    }

    private <T extends RepresentationModel<T>> PagedModel<T> getPage(String uri, PagedModelType<T> responseType,
                                                                     Object... uriVariables) {
        return restTemplate.exchange(uri, HttpMethod.GET, null, responseType, uriVariables).getBody();
    }

    private <T extends RepresentationModel<T>> PagedModel<T> getPage(URI uri, PagedModelType<T> responseType) {
        return restTemplate.exchange(uri, HttpMethod.GET, null, responseType).getBody();
    }

    private ApiException buildApiException(RestClientResponseException e) {
        ResponseBodyException responseBody;
        try {
            responseBody = objectMapper.readValue(e.getResponseBodyAsString(), ResponseBodyException.class);
        } catch (JsonProcessingException je) {
            return new ApiException(e);
        }
        return new ApiException(responseBody.getMessage() + " (status code: " + e.getRawStatusCode() + ")");
    }

    private String getDatasetUrl() {
        return (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + DATASET_PATH;
    }

}
