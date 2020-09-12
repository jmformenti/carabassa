package org.atypical.carabassa.restapi.rdbms.mapper;

import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = BoundingBoxMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper extends org.atypical.carabassa.restapi.mapper.TagMapper {

	public TagEntity toEntity(TagEditableRepresentation tagEditableRepresentation);

}
