package org.atypical.carabassa.restapi.rdbms.mapper;

import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = TagMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ImageMapper extends org.atypical.carabassa.restapi.mapper.ImageMapper {

	public ImageRepresentation toRepresentation(IndexedImage indexedImage);

}
