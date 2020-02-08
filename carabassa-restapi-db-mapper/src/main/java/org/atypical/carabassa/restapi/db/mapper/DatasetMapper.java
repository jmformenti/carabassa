package org.atypical.carabassa.restapi.db.mapper;

import org.atypical.carabassa.core.db.entity.DatasetEntity;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface DatasetMapper extends org.atypical.carabassa.restapi.mapper.DatasetMapper {

	@BeanMapping(ignoreByDefault = true)
	public DatasetRepresentation toRepresentation(Dataset dataset);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "creation", ignore = true)
	@Mapping(target = "modification", ignore = true)
	@Mapping(target = "images", ignore = true)
	public DatasetEntity toEntity(DatasetRepresentation datasetDTO);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "name", ignore = true)
	@Mapping(target = "creation", ignore = true)
	@Mapping(target = "modification", ignore = true)
	@Mapping(target = "images", ignore = true)
	public void update(DatasetRepresentation datasetDto, @MappingTarget Dataset dataset);

}
