package org.atypical.carabassa.cli.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ResponseBodyException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DatasetApiServiceImpl implements DatasetApiService {

	private final static String BASE_URL = "http://localhost:8080/api/";
	private final static String DATASET_URL = BASE_URL + "dataset/";

	@Autowired
	private RestTemplate restTemplate;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Long create(String name, String description) throws JsonProcessingException, ApiException {
		Assert.notNull(name, "Name can not be null.");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		DatasetRepresentation dataset = new DatasetRepresentation(name);
		dataset.setDescription(description);

		HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(dataset), headers);

		ResponseEntity<Long> response = null;
		try {
			response = restTemplate.postForEntity(DATASET_URL, request, Long.class);
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
		return response.getBody();
	}

	@Override
	public void delete(Long datasetId) throws JsonProcessingException, ApiException {
		try {
			restTemplate.delete(DATASET_URL + "{datasetId}", datasetId);
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
	}

	@Override
	public List<DatasetRepresentation> findAll() throws URISyntaxException, JsonProcessingException, ApiException {
		try {
			PagedModel<DatasetRepresentation> page = getPage(new URI(DATASET_URL),
					new PagedModelType<DatasetRepresentation>() {
					});

			List<DatasetRepresentation> datasets = new ArrayList<>(page.getContent());
			while (page.hasLink(IanaLinkRelations.NEXT)) {
				page = getPage(page.getLink("next").get().toUri(), new PagedModelType<DatasetRepresentation>() {
				});
				datasets.addAll(page.getContent());
			}

			return datasets;
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
	}

	@Override
	public Long findByName(String datasetName) throws JsonProcessingException, ApiException {
		ResponseEntity<DatasetRepresentation> response = null;
		try {
			response = restTemplate.getForEntity(DATASET_URL + "name/{datasetName}", DatasetRepresentation.class,
					datasetName);
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
		return response.getBody().getId();
	}

	@Override
	public void update(Long datasetId, String description) throws JsonProcessingException, ApiException {
		try {
			DatasetRepresentation dataset = new DatasetRepresentation();
			dataset.setDescription(description);
			restTemplate.put(DATASET_URL + "{datasetId}", dataset, datasetId);
		} catch (RestClientResponseException e) {
			throw buildApiException(e);
		}
	}

	private <T extends DatasetRepresentation> PagedModel<T> getPage(URI uri, PagedModelType<T> responseType) {
		return restTemplate.exchange(uri, HttpMethod.GET, null, responseType).getBody();
	}

	private ApiException buildApiException(RestClientResponseException e) throws JsonProcessingException {
		ResponseBodyException responseBody = objectMapper.readValue(e.getResponseBodyAsString(),
				ResponseBodyException.class);
		return new ApiException(responseBody.getMessage());
	}

}
