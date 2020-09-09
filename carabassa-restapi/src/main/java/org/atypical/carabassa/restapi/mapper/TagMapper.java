package org.atypical.carabassa.restapi.mapper;

import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;

public interface TagMapper {

	public Tag toEntity(TagEditableRepresentation tagEditableRepresentation);

}
