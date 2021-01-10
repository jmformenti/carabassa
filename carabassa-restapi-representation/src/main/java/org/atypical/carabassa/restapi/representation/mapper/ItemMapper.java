package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;

public interface ItemMapper {

	public ItemRepresentation toRepresentation(IndexedItem indexedItem);

}
