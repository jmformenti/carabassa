package org.atypical.carabassa.restapi.db.mapper;

import org.atypical.carabassa.core.db.entity.BoundingBoxEntity;
import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.restapi.representation.model.BoundingBoxRepresentation;
import org.mapstruct.Mapper;

@Mapper
public interface BoundingBoxMapper extends org.atypical.carabassa.restapi.mapper.BoundingBoxMapper {

	public BoundingBoxRepresentation toRepresentation(BoundingBox boundingBox);

	public BoundingBoxEntity toEntity(BoundingBoxRepresentation boundingBoxRepresentation);

}
