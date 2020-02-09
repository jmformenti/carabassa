package org.atypical.carabassa.restapi.db.mapper;

import org.atypical.carabassa.core.db.entity.DatasetEntity;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.atypical.carabassa.restapi.representation.model.NewDatasetRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DatasetMapper extends org.atypical.carabassa.restapi.mapper.DatasetMapper {

	public DatasetRepresentation toRepresentation(Dataset dataset);

	public DatasetEntity toEntity(NewDatasetRepresentation newDatasetRepresentation);

	public DatasetEntity toEntity(DatasetRepresentation datasetRepresentation);

	public void update(NewDatasetRepresentation datasetRepresentation, @MappingTarget Dataset dataset);

}
