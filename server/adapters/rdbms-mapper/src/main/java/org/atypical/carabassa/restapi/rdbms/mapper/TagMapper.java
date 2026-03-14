package org.atypical.carabassa.restapi.rdbms.mapper;

import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = BoundingBoxMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper extends org.atypical.carabassa.restapi.representation.mapper.TagMapper {

    @Override
    @Mapping(target = "value", ignore = true)
    @Mapping(target = "valueType", source = "type")
    TagEntity toEntity(TagEditableRepresentation tagEditableRepresentation);

    @AfterMapping
    default void mapValue(TagEditableRepresentation source, @org.mapstruct.MappingTarget TagEntity target) {
        target.setValue(source.getValue(), target.getValueType());
    }

    @Override
    TagEntityRepresentation toRepresentation(org.atypical.carabassa.core.model.Tag tag);

}
