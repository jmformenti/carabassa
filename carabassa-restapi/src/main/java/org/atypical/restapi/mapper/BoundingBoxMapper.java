package org.atypical.restapi.mapper;

import org.atypical.core.model.BoundingBox;
import org.atypical.core.model.impl.BoundingBoxImpl;
import org.atypical.restapi.dto.BoundingBoxDto;
import org.mapstruct.Mapper;

@Mapper
public interface BoundingBoxMapper {

	public BoundingBoxDto toDTO(BoundingBox boundingBox);

	public BoundingBoxImpl toEntity(BoundingBoxDto boundingBoxDto);

}
