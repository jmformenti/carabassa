package org.atypical.carabassa.core.model;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.atypical.carabassa.core.model.enums.ItemType;

public interface IndexedItem {

	public Long getId();

	public void setId(Long id);

	public ItemType getType();

	public void setType(ItemType type);

	public String getFilename();

	public void setFilename(String filename);

	public String getFormat();

	public void setFormat(String format);

	public String getHash();

	public void setHash(String hash);

	public ZonedDateTime getCreation();

	public void setCreation(ZonedDateTime creation);

	public ZonedDateTime getModification();

	public void setModification(ZonedDateTime modification);

	public ZonedDateTime getArchiveTime();

	public void setArchiveTime(ZonedDateTime archiveTime);

	public Set<Tag> getTags();

	public void setTags(Set<Tag> tags);

	public default Set<Tag> getTags(String name) {
		if (name != null) {
			return getTags().stream().filter(t -> name.equals(t.getName())).collect(Collectors.toSet());
		} else {
			return null;
		}
	}

	public default Tag getFirstTag(String name) {
		if (name != null) {
			return getTags().stream().filter(t -> name.equals(t.getName())).findFirst().orElse(null);
		} else {
			return null;
		}
	}

	public default boolean isArchived() {
		return getArchiveTime() != null;
	}

	public Dataset getDataset();

	public void setDataset(Dataset dataset);

}
