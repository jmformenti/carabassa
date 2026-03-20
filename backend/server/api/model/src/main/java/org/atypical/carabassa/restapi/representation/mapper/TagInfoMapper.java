package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.restapi.representation.model.TagInfoEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagInfoEntityRepresentation;

public interface TagInfoMapper {

    TagInfo toEntity(TagInfoEditableRepresentation tagInfoEditableRepresentation);

    TagInfoEntityRepresentation toRepresentation(TagInfo tagInfo);
}
