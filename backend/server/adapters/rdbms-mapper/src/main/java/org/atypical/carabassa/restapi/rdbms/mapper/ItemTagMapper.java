package org.atypical.carabassa.restapi.rdbms.mapper;

import org.atypical.carabassa.core.model.ItemTagInfo;
import org.atypical.carabassa.restapi.representation.model.ItemTagEntityRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemTagMapper extends org.atypical.carabassa.restapi.representation.mapper.ItemTagMapper {

    @Override
    @Mapping(target = "tagId", source = "tag.id")
    @Mapping(target = "tagName", source = "tag.name")
    @Mapping(target = "tagValue", source = "tag.value")
    ItemTagEntityRepresentation toRepresentation(ItemTagInfo itemTagInfo);

}
