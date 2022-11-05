package org.atypical.carabassa.core.model;

import java.time.Instant;
import java.util.Set;

public interface Dataset {

	public Long getId();

	public void setId(Long id);

	public String getName();

	public void setName(String name);

	public String getDescription();

	public void setDescription(String description);

	public Instant getCreation();

	public void setCreation(Instant creation);

	public Instant getModification();

	public void setModification(Instant modification);

	public Set<IndexedItem> getItems();

	public void setItems(Set<IndexedItem> items);

}
