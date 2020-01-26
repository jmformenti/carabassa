package org.atypical.carabassa.core.model;

import java.time.ZonedDateTime;
import java.util.Set;

public interface IndexedImage {

	public Long getId();

	public void setId(Long id);

	public String getFilename();

	public void setFilename(String filename);

	public String getFileType();

	public void setFileType(String fileType);

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

	public Set<Tag> getTags(String name);

	public Tag getFirstTag(String name);

	public boolean isArchived();

	public Dataset getDataset();

	public void setDataset(Dataset dataset);

}
