package org.atypical.carabassa.restapi.representation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class NewTagRepresentation {

	private String name;
	private Object value;
	@JsonInclude(Include.NON_NULL)
	private BoundingBoxRepresentation boundingBox;

	public NewTagRepresentation(String name, Object value, BoundingBoxRepresentation boundingBox) {
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
