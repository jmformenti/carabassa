package org.atypical.carabassa.restapi.db.mapper;

import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.core.model.impl.BoundingBoxImpl;
import org.atypical.carabassa.restapi.representation.model.BoundingBoxRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BoundingBoxMapper extends org.atypical.carabassa.restapi.mapper.BoundingBoxMapper {

	public BoundingBoxRepresentation toRepresentation(BoundingBox boundingBox);

	public BoundingBoxImpl toEntity(BoundingBoxRepresentation boundingBoxRepresentation);

}
