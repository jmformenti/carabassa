package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;

public interface ImageMapper {

	public ImageRepresentation toRepresentation(IndexedImage indexedImage);

}
