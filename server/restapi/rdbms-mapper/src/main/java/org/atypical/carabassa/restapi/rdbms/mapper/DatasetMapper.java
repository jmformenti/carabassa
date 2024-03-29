package org.atypical.carabassa.restapi.rdbms.mapper;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.indexer.rdbms.entity.DatasetEntity;
import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DatasetMapper extends org.atypical.carabassa.restapi.representation.mapper.DatasetMapper {

    @Override
    DatasetEntityRepresentation toRepresentation(Dataset dataset);

    @Override
    DatasetEntity toEntity(DatasetEditableRepresentation datasetEditableRepresentation);

    @Override
    void update(DatasetEditableRepresentation datasetEditableRepresentation, @MappingTarget Dataset dataset);

}
