package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;

public interface DatasetMapper {

	DatasetEntityRepresentation toRepresentation(Dataset dataset);

	Dataset toEntity(DatasetEditableRepresentation datasetEditableRepresentation);

	void update(DatasetEditableRepresentation datasetEditableRepresentation, Dataset dataset);

}
