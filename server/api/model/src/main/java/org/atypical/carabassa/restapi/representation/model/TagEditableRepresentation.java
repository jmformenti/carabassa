package org.atypical.carabassa.restapi.representation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class TagEditableRepresentation {

    @NotEmpty(message = "{api.dataset.item.tag.name.notEmpty}")
    private String name;
    @NotNull(message = "{api.dataset.item.tag.value.notNull}")
    private Object value;
    @JsonInclude(Include.NON_NULL)
    private BoundingBoxRepresentation boundingBox;
    @JsonInclude(Include.NON_NULL)
    private String type;

    public TagEditableRepresentation() {
        super();
    }

    public TagEditableRepresentation(String name, Object value, BoundingBoxRepresentation boundingBox) {
        this(name, value, boundingBox, null);
    }

    public TagEditableRepresentation(String name, Object value, BoundingBoxRepresentation boundingBox, String type) {
        super();
        this.name = name;
        this.value = value;
        this.boundingBox = boundingBox;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
