package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.db.entity.TagEntity;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.restapi.dto.TagDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = BoundingBoxMapper.class)
public interface TagMapper {

	public TagDto toDTO(Tag tag);

	@Mapping(target = "id", ignore = true)
	public TagEntity toEntity(TagDto tagDto);

}
