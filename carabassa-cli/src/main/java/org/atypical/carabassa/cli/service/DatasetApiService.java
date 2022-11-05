package org.atypical.carabassa.cli.service;

import java.io.IOException;
import java.util.List;

import org.atypical.carabassa.cli.dto.ItemToUpload;
import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ItemAlreadyExists;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;

// TODO Do junits
public interface DatasetApiService {

	public Long addItem(Long datasetId, ItemToUpload itemToUpload) throws ItemAlreadyExists, ApiException, IOException;

	public Long create(String name, String description) throws ApiException, JsonProcessingException;

	public void delete(Long datasetId) throws ApiException;

	public List<DatasetEntityRepresentation> findAll() throws ApiException;

	public Long findByName(String datasetName) throws ApiException;
	
	public List<ItemRepresentation> findItems(Long datasetId) throws ApiException;

	public void update(Long datasetId, String description) throws ApiException;

}
