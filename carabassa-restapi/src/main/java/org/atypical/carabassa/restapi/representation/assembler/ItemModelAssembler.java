package org.atypical.carabassa.restapi.representation.assembler;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.representation.mapper.ItemMapper;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class ItemModelAssembler extends RepresentationModelAssemblerSupport<IndexedItem, ItemRepresentation> {

	@Autowired
	private ItemMapper itemMapper;

	public ItemModelAssembler() {
		super(DatasetController.class, ItemRepresentation.class);
	}

	@Override
	public ItemRepresentation toModel(IndexedItem indexedItem) {
		return itemMapper.toRepresentation(indexedItem);
	}

}
