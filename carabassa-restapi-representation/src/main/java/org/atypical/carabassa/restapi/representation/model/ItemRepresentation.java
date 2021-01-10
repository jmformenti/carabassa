package org.atypical.carabassa.restapi.representation.model;

import java.time.ZonedDateTime;
import java.util.SortedSet;

import org.springframework.hateoas.RepresentationModel;

public class ItemRepresentation extends RepresentationModel<ItemRepresentation> {

	private Long id;
	private String type;
	private String filename;
	private String format;
	private String hash;
	private ZonedDateTime creation;
	private ZonedDateTime modification;
	private ZonedDateTime archiveTime;
	private SortedSet<TagEntityRepresentation> tags;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
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

	public ZonedDateTime getArchiveTime() {
		return archiveTime;
	}

	public void setArchiveTime(ZonedDateTime archiveTime) {
		this.archiveTime = archiveTime;
	}

	public SortedSet<TagEntityRepresentation> getTags() {
		return tags;
	}

	public void setTags(SortedSet<TagEntityRepresentation> tags) {
		this.tags = tags;
	}

}
