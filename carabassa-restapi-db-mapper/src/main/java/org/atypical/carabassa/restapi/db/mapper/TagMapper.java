package org.atypical.carabassa.restapi.db.mapper;

import org.atypical.carabassa.core.db.entity.TagEntity;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.restapi.representation.model.NewTagRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = BoundingBoxMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper extends org.atypical.carabassa.restapi.mapper.TagMapper {

	public TagRepresentation toRepresentation(Tag tag);

	public TagEntity toEntity(TagRepresentation tagRepresentation);

	public TagEntity toEntity(NewTagRepresentation newTagRepresentation);

}
