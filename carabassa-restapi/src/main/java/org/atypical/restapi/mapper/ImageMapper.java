package org.atypical.restapi.mapper;

import java.util.Collection;
import java.util.List;

import org.atypical.core.model.IndexedImage;
import org.atypical.restapi.dto.ImageDto;
import org.mapstruct.Mapper;

@Mapper(uses = TagMapper.class)
public interface ImageMapper {

	public ImageDto toDTO(IndexedImage indexedImage);

	public List<ImageDto> toDTO(Collection<IndexedImage> images);

}
