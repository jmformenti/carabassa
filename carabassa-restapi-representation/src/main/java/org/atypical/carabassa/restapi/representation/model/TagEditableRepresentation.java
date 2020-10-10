package org.atypical.carabassa.restapi.representation.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TagEditableRepresentation {

	@NotEmpty(message = "{api.dataset.image.tag.name.notEmpty}")
	private String name;
	@NotNull(message = "{api.dataset.image.tag.value.notNull}")
	private Object value;
	@JsonInclude(Include.NON_NULL)
	private BoundingBoxRepresentation boundingBox;

	public TagEditableRepresentation(String name, Object value, BoundingBoxRepresentation boundingBox) {
		super();
		this.name = name;
		this.value = value;
		this.boundingBox = boundingBox;
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

}
