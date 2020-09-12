package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.restapi.representation.model.BoundingBoxRepresentation;

public interface BoundingBoxMapper {

	public BoundingBoxRepresentation toRepresentation(BoundingBox boundingBox);

	public BoundingBox toEntity(BoundingBoxRepresentation boundingBoxRepresentation);

}
