package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;

public interface DatasetMapper {

	public DatasetRepresentation toRepresentation(Dataset dataset);

	public Dataset toEntity(DatasetRepresentation datasetDTO);

	public void update(DatasetRepresentation datasetDto, Dataset dataset);

}
