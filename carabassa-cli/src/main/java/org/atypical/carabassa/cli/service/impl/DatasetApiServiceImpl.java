package org.atypical.carabassa.cli.service.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ResponseBodyException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.core.TypeReferences.PagedModelType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DatasetApiServiceImpl implements DatasetApiService {

	private final static String DATASET_PATH = "dataset/";

	@Value("${carabassa.baseUrl}")
	private String baseUrl;

	@Autowired
	private RestTemplate restTemplate;

	private ObjectMapper objectMapper = new ObjectMapper();

	private String datasetUrl;

	@PostConstruct
	private void postConstruct() {
		datasetUrl = getDatasetUrl();
	}

	@Override
	public Long addImage(String datasetName, Path imagePath) throws ApiException {
		Long datasetId = findByName(datasetName);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new FileSystemResource(imagePath));

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

		ResponseEntity<EntityModel<Long>> response = null;
		try {
			response = restTemplate.exchange(datasetUrl + "{datasetId}/image", HttpMethod.POST, request,
					new ParameterizedTypeReference<EntityModel<Long>>() {
					}, datasetId);
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
		return response.getBody().getContent();
	}

	@Override
	public Long create(String name, String description) throws ApiException, JsonProcessingException {
		Assert.notNull(name, "Name can not be null.");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		DatasetRepresentation dataset = new DatasetRepresentation(name);
		dataset.setDescription(description);

		HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(dataset), headers);

		ResponseEntity<EntityModel<Long>> response = null;
		try {
			response = restTemplate.exchange(datasetUrl, HttpMethod.POST, request,
					new ParameterizedTypeReference<EntityModel<Long>>() {
					});
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
		return response.getBody().getContent();
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
	public List<DatasetRepresentation> findAll() throws ApiException {
		try {
			PagedModel<DatasetRepresentation> page = getPage(datasetUrl, new PagedModelType<DatasetRepresentation>() {
			});

			List<DatasetRepresentation> datasets = new ArrayList<>(page.getContent());
			while (page.hasLink(IanaLinkRelations.NEXT)) {
				page = getPage(page.getLink("next").get().toUri().toString(),
						new PagedModelType<DatasetRepresentation>() {
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
		ResponseEntity<DatasetRepresentation> response = null;
		try {
			response = restTemplate.getForEntity(datasetUrl + "name/{datasetName}", DatasetRepresentation.class,
					datasetName);
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
		return response.getBody().getId();
	}

	@Override
	public void update(Long datasetId, String description) throws ApiException {
		try {
			DatasetRepresentation dataset = new DatasetRepresentation();
			dataset.setDescription(description);
			restTemplate.put(datasetUrl + "{datasetId}", dataset, datasetId);
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
	}

	private <T extends DatasetRepresentation> PagedModel<T> getPage(String uri, PagedModelType<T> responseType) {
		return restTemplate.exchange(uri, HttpMethod.GET, null, responseType).getBody();
	}

	private ApiException buildApiException(RestClientResponseException e) {
		ResponseBodyException responseBody = null;
		try {
			responseBody = objectMapper.readValue(e.getResponseBodyAsString(), ResponseBodyException.class);
		} catch (JsonProcessingException je) {
			return new ApiException(e);
		}
		return new ApiException(responseBody.getMessage());
	}

	private String getDatasetUrl() {
		return (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + DATASET_PATH;
	}
}
