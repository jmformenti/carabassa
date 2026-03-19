package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.restapi.representation.model.BoundingBoxRepresentation;

public interface BoundingBoxMapper {

    BoundingBoxRepresentation toRepresentation(BoundingBox boundingBox);

    BoundingBox toEntity(BoundingBoxRepresentation boundingBoxRepresentation);

}
