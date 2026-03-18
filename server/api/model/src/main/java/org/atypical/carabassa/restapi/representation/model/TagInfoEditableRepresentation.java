package org.atypical.carabassa.restapi.representation.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.atypical.carabassa.core.model.enums.ValueType;

@Getter
@Setter
public class TagInfoEditableRepresentation {

    @NotEmpty(message = "{api.taginfo.tagName.notEmpty}")
    private String tagName;
    private String description;
    private String alias;
    private Boolean internal;
    private Boolean sortable;
    private ValueType type;

    public TagInfoEditableRepresentation() {
        super();
    }

    public TagInfoEditableRepresentation(String tagName, String description, String alias, Boolean internal,
                                         Boolean sortable, ValueType type) {
        this.tagName = tagName;
        this.description = description;
        this.alias = alias;
        this.internal = internal;
        this.sortable = sortable;
        this.type = type;
    }
}
