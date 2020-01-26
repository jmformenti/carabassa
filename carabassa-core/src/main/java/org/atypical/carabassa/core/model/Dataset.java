package org.atypical.carabassa.core.model;

import java.time.ZonedDateTime;
import java.util.Set;

public interface Dataset {

	public Long getId();

	public void setId(Long id);

	public String getName();

	public void setName(String name);

	public String getDescription();

	public void setDescription(String description);

	public ZonedDateTime getCreation();

	public void setCreation(ZonedDateTime creation);

	public ZonedDateTime getModification();

	public void setModification(ZonedDateTime modification);

	public Set<IndexedImage> getImages();

	public void setImages(Set<IndexedImage> images);

}
