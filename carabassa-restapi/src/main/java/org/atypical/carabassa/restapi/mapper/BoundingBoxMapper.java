package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.core.model.impl.BoundingBoxImpl;
import org.atypical.carabassa.restapi.representation.model.BoundingBoxRepresentation;

public interface BoundingBoxMapper {

	public BoundingBoxRepresentation toRepresentation(BoundingBox boundingBox);

	public BoundingBoxImpl toEntity(BoundingBoxRepresentation boundingBoxRepresentation);

}
