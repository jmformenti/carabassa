package org.atypical.restapi.mapper;

import org.atypical.core.db.entity.DatasetEntity;
import org.atypical.core.model.Dataset;
import org.atypical.restapi.dto.DatasetDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface DatasetMapper {

	public DatasetDto toDTO(Dataset dataset);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "creation", ignore = true)
	@Mapping(target = "modification", ignore = true)
	@Mapping(target = "images", ignore = true)
	public DatasetEntity toEntity(DatasetDto datasetDTO);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "creation", ignore = true)
	@Mapping(target = "modification", ignore = true)
	@Mapping(target = "images", ignore = true)
	public void update(DatasetDto datasetDto, @MappingTarget Dataset dataset);

}
