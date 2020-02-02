package org.atypical.carabassa.restapi.db.mapper;

import org.atypical.carabassa.core.db.entity.TagEntity;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.restapi.representation.model.TagRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = BoundingBoxMapper.class)
public interface TagMapper extends org.atypical.carabassa.restapi.mapper.TagMapper {

	@Mapping(target = "add", ignore = true)
	public TagRepresentation toRepresentation(Tag tag);

	@Mapping(target = "id", ignore = true)
	public TagEntity toEntity(TagRepresentation tagDto);

}
