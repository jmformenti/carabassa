package org.atypical.carabassa.restapi.representation.model;

import jakarta.validation.constraints.NotEmpty;

public class TagInfoEditableRepresentation {

    @NotEmpty(message = "{api.taginfo.tagName.notEmpty}")
    private String tagName;
    private String description;
    private String alias;
    private Boolean internal;

    public TagInfoEditableRepresentation() {
        super();
    }

    public TagInfoEditableRepresentation(String tagName, String description, String alias, Boolean internal) {
        this.tagName = tagName;
        this.description = description;
        this.alias = alias;
        this.internal = internal;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Boolean getInternal() {
        return internal;
    }

    public void setInternal(Boolean internal) {
        this.internal = internal;
    }
}
