package org.atypical.carabassa.restapi.representation.mapper;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;

public interface ItemMapper {

    ItemRepresentation toBaseRepresentation(IndexedItem indexedItem);

    ItemRepresentation toRepresentation(IndexedItem indexedItem);

}
