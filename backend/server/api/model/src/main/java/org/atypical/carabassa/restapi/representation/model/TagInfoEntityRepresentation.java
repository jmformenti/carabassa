package org.atypical.carabassa.restapi.representation.model;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import lombok.Getter;
import lombok.Setter;
import org.atypical.carabassa.core.model.enums.ValueType;

@Relation(collectionRelation = "tagInfoEntityRepresentationList")
@Getter
@Setter
public class TagInfoEntityRepresentation extends RepresentationModel<TagInfoEntityRepresentation> {

    private Long id;
    private String tagName;
    private String description;
    private String alias;
    private Boolean internal;
    private Boolean sortable;
    private Boolean showInHelp;
    private ValueType type;

    public TagInfoEntityRepresentation() {
        super();
    }
}
