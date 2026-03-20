package org.atypical.carabassa.restapi.representation.model;

import jakarta.validation.constraints.NotEmpty;

public class DatasetEditableRepresentation {

    @NotEmpty(message = "{api.dataset.name.notEmpty}")
    private String name;
    private String description;

    public DatasetEditableRepresentation() {
        super();
    }

    public DatasetEditableRepresentation(String name, String description) {
        super();
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
