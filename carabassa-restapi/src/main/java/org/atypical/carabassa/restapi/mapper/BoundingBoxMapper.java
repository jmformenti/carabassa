package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.core.model.impl.BoundingBoxImpl;
import org.atypical.carabassa.restapi.dto.BoundingBoxDto;
import org.mapstruct.Mapper;

@Mapper
public interface BoundingBoxMapper {

	public BoundingBoxDto toDTO(BoundingBox boundingBox);

	public BoundingBoxImpl toEntity(BoundingBoxDto boundingBoxDto);

}
