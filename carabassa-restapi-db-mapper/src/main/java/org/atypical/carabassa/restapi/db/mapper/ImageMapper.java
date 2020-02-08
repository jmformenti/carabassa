package org.atypical.carabassa.restapi.db.mapper;

import java.util.Collection;
import java.util.List;

import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(uses = TagMapper.class)
public interface ImageMapper extends org.atypical.carabassa.restapi.mapper.ImageMapper {

	@BeanMapping(ignoreByDefault = true)
	public ImageRepresentation toRepresentation(IndexedImage indexedImage);

	public List<ImageRepresentation> toRepresentation(Collection<IndexedImage> images);

}
