package org.atypical.carabassa.restapi.representation.assembler;

import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.representation.mapper.ImageMapper;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class ImageModelAssembler extends RepresentationModelAssemblerSupport<IndexedImage, ImageRepresentation> {

	@Autowired
	private ImageMapper imageMapper;

	public ImageModelAssembler() {
		super(DatasetController.class, ImageRepresentation.class);
	}

	@Override
	public ImageRepresentation toModel(IndexedImage indexedImage) {
		return imageMapper.toRepresentation(indexedImage);
	}

}
