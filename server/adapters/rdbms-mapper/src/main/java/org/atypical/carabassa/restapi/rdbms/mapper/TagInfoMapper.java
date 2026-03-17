package org.atypical.carabassa.restapi.rdbms.mapper;

import org.atypical.carabassa.indexer.rdbms.entity.TagInfoEntity;
import org.atypical.carabassa.restapi.representation.model.TagInfoEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagInfoEntityRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagInfoMapper extends org.atypical.carabassa.restapi.representation.mapper.TagInfoMapper {

    @Override
    TagInfoEntity toEntity(TagInfoEditableRepresentation tagInfoEditableRepresentation);

    @Override
    TagInfoEntityRepresentation toRepresentation(org.atypical.carabassa.core.model.TagInfo tagInfo);
}
