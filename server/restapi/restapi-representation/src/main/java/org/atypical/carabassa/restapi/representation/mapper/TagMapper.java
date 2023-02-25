package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;

public interface TagMapper {

	Tag toEntity(TagEditableRepresentation tagEditableRepresentation);

}
