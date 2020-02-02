package org.atypical.carabassa.restapi.mapper;

import java.util.Collection;
import java.util.List;

import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;

public interface ImageMapper {

	public ImageRepresentation toRepresentation(IndexedImage indexedImage);

	public List<ImageRepresentation> toDTO(Collection<IndexedImage> images);

}
