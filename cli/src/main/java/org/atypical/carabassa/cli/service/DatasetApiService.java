package org.atypical.carabassa.cli.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.atypical.carabassa.cli.dto.ItemToUpload;
import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ItemAlreadyExists;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;

import java.io.IOException;
import java.util.List;

// TODO Do junits
public interface DatasetApiService {

    Long addItem(Long datasetId, ItemToUpload itemToUpload) throws ItemAlreadyExists, ApiException, IOException;

    Long create(String name, String description) throws ApiException, JsonProcessingException;

    void delete(Long datasetId) throws ApiException;

    void deleteItem(Long datasetId, Long id) throws ApiException;

    List<DatasetEntityRepresentation> findAll() throws ApiException;

    Long findByName(String datasetName) throws ApiException;

    List<ItemRepresentation> findItems(Long datasetId) throws ApiException;

    List<ItemRepresentation> findItems(Long datasetId, String searchString) throws ApiException;

    void reindex(Long datasetId, Long id) throws ApiException;

    void update(Long datasetId, String name, String description) throws ApiException;

}
