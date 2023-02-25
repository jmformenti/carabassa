package org.atypical.carabassa.restapi.representation.model;

import org.springframework.hateoas.RepresentationModel;

public class IdRepresentation extends RepresentationModel<IdRepresentation> {

    private Long id;

    public IdRepresentation() {
        super();
    }

    public IdRepresentation(Long id) {
        super();
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
