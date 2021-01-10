package org.atypical.carabassa.core.model.impl;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;

public class DatasetImpl implements Dataset {

	private Long id;
	private String name;
	private String description;
	private ZonedDateTime creation;
	private ZonedDateTime modification;
	private Set<IndexedItem> items;

	public DatasetImpl() {
		super();
		this.items = new HashSet<>();
	}

	public DatasetImpl(String name) {
		this();
		this.name = name;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public ZonedDateTime getCreation() {
		return creation;
	}

	@Override
	public void setCreation(ZonedDateTime creation) {
		this.creation = creation;
	}

	@Override
	public ZonedDateTime getModification() {
		return modification;
	}

	@Override
	public void setModification(ZonedDateTime modification) {
		this.modification = modification;
	}

	@Override
	public Set<IndexedItem> getItems() {
		return items;
	}

	@Override
	public void setItems(Set<IndexedItem> items) {
		this.items = items;
	}

}
