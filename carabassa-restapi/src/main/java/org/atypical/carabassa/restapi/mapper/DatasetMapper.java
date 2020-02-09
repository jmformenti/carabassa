package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.atypical.carabassa.restapi.representation.model.NewDatasetRepresentation;

public interface DatasetMapper {

	public DatasetRepresentation toRepresentation(Dataset dataset);

	public Dataset toEntity(NewDatasetRepresentation newDatasetRepresentation);

	public Dataset toEntity(DatasetRepresentation datasetRepresentation);

	public void update(NewDatasetRepresentation datasetRepresentation, Dataset dataset);

}
