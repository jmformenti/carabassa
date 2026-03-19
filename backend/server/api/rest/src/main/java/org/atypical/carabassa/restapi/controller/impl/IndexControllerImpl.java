package org.atypical.carabassa.restapi.controller.impl;

import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.controller.IndexController;
import org.atypical.carabassa.restapi.representation.model.NoContentRepresentation;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class IndexControllerImpl implements IndexController {

    @Override
    public NoContentRepresentation index() {
        return new NoContentRepresentation(linkTo(DatasetController.class).withRel("datasets"));
    }
}
