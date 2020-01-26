package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.core.model.Tag;

public class TagImpl implements Tag {

	private Long id;
	private String name;
	private Object value;
	private BoundingBox boundingBox;

	public TagImpl(String name, Object value) {
		super();
		this.name = name;
		this.value = value;
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
	public Object getValue() {
		return value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(Class<T> clazz) {
		return (T) value;
	}

	@Override
	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}

}
