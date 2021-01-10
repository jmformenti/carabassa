package org.atypical.carabassa.restapi.rdbms.mapper;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = TagMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemMapper extends org.atypical.carabassa.restapi.representation.mapper.ItemMapper {

	default String itemTypeConverter(ItemType value) {
		return value.normalized();
	}

	public ItemRepresentation toRepresentation(IndexedItem indexedItem);

}
