package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;

public interface DatasetMapper {

	public DatasetEntityRepresentation toRepresentation(Dataset dataset);

	public Dataset toEntity(DatasetEditableRepresentation datasetEditableRepresentation);

	public void update(DatasetEditableRepresentation datasetEditableRepresentation, Dataset dataset);

}
