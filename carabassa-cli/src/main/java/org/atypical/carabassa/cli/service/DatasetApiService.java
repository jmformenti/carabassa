package org.atypical.carabassa.cli.service;

import java.net.URISyntaxException;
import java.util.List;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface DatasetApiService {

	public Long create(String name, String description) throws JsonProcessingException, ApiException;

	public void delete(Long datasetId) throws JsonProcessingException, ApiException;

	public List<DatasetRepresentation> findAll() throws URISyntaxException, JsonProcessingException, ApiException;

	public Long findByName(String datasetName) throws JsonProcessingException, ApiException;

	public void update(Long datasetId, String description) throws JsonProcessingException, ApiException;

}
