package org.atypical.carabassa.restapi.representation.model;

import java.time.ZonedDateTime;

import org.springframework.hateoas.RepresentationModel;

public class DatasetRepresentation extends RepresentationModel<DatasetRepresentation> {

	private Long id;
	private String name;
	private String description;
	private ZonedDateTime creation;
	private ZonedDateTime modification;

	public DatasetRepresentation() {
		super();
	}

	public DatasetRepresentation(String name) {
		super();
		this.name = name;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ZonedDateTime getCreation() {
		return creation;
	}

	public void setCreation(ZonedDateTime creation) {
		this.creation = creation;
	}

	public ZonedDateTime getModification() {
		return modification;
	}

	public void setModification(ZonedDateTime modification) {
		this.modification = modification;
	}

}
