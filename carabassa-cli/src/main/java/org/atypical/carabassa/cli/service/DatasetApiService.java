package org.atypical.carabassa.cli.service;

import java.nio.file.Path;
import java.util.List;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface DatasetApiService {

	public Long addImage(String datasetName, Path imagePath) throws ApiException;

	public Long create(String name, String description) throws ApiException, JsonProcessingException;

	public void delete(Long datasetId) throws ApiException;

	public List<DatasetRepresentation> findAll() throws ApiException;

	public Long findByName(String datasetName) throws ApiException;

	public void update(Long datasetId, String description) throws ApiException;

}
