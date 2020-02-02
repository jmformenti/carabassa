package org.atypical.carabassa.core.model;

import java.time.ZonedDateTime;

public interface Tag {

	public Long getId();

	public void setId(Long id);

	public ZonedDateTime getCreation();

	public String getName();

	public void setName(String name);

	public Object getValue();

	public <T> T getValue(Class<T> clazz);

	public void setValue(Object value);

	public BoundingBox getBoundingBox();

	public void setBoundingBox(BoundingBox boundingBox);

}
