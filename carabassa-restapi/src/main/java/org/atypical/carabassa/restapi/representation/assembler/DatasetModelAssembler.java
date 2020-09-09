package org.atypical.carabassa.restapi.representation.assembler;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class DatasetModelAssembler extends RepresentationModelAssemblerSupport<Dataset, DatasetEntityRepresentation> {

	@Autowired
	private DatasetMapper datasetMapper;

	public DatasetModelAssembler() {
		super(DatasetController.class, DatasetEntityRepresentation.class);
	}

	@Override
	public DatasetEntityRepresentation toModel(Dataset dataset) {
		return datasetMapper.toRepresentation(dataset);
	}

}
