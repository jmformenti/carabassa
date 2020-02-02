package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.restapi.representation.model.TagRepresentation;

public interface TagMapper {

	public TagRepresentation toRepresentation(Tag tag);

	public Tag toEntity(TagRepresentation tagDto);

}
