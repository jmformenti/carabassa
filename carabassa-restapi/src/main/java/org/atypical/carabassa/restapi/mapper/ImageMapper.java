package org.atypical.carabassa.restapi.mapper;

import java.util.Collection;
import java.util.List;

import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.restapi.dto.ImageDto;
import org.mapstruct.Mapper;

@Mapper(uses = TagMapper.class)
public interface ImageMapper {

	public ImageDto toDTO(IndexedImage indexedImage);

	public List<ImageDto> toDTO(Collection<IndexedImage> images);

}
