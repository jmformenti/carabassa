package org.atypical.carabassa.restapi.representation.model;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TagEntityRepresentation extends RepresentationModel<TagEntityRepresentation>
        implements Comparable<TagEntityRepresentation> {

    private Long id;
    private String name;
    private Object value;
    @JsonInclude(Include.NON_NULL)
    private BoundingBoxRepresentation boundingBox;

    public TagEntityRepresentation() {
        super();
    }

    public TagEntityRepresentation(Long id, String name, Object value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public BoundingBoxRepresentation getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBoxRepresentation boundingBox) {
        this.boundingBox = boundingBox;
    }

    @Override
    public int compareTo(TagEntityRepresentation o) {
        return getId().compareTo(o.getId());
    }

}
