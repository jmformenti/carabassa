package org.atypical.carabassa.restapi.representation.model;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

public class NoContentRepresentation extends RepresentationModel<NoContentRepresentation> {

	public NoContentRepresentation(Link link) {
		super(link);
	}
}
