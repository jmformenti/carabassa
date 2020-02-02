package org.atypical.carabassa.restapi.controller.impl;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.controller.RootController;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

@Component
public class RootControllerImpl implements RootController {

	@Override
	public EntityModel<String> actions() {
		return new EntityModel<String>("", linkTo(DatasetController.class).withRel("datasets"));
	}
}
