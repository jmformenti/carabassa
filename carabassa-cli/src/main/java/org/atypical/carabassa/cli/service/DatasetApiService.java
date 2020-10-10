package org.atypical.carabassa.cli.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ImageAlreadyExists;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;

// TODO Do junits
public interface DatasetApiService {

	public Long addImage(Long datasetId, Path imagePath) throws ImageAlreadyExists, ApiException, IOException;

	public Long create(String name, String description) throws ApiException, JsonProcessingException;

	public void delete(Long datasetId) throws ApiException;

	public List<DatasetEntityRepresentation> findAll() throws ApiException;

	public Long findByName(String datasetName) throws ApiException;

	public void update(Long datasetId, String description) throws ApiException;

}
