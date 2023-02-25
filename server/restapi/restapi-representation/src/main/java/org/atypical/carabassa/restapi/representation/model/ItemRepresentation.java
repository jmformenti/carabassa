package org.atypical.carabassa.restapi.representation.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.SortedSet;

import org.springframework.hateoas.RepresentationModel;

public class ItemRepresentation extends RepresentationModel<ItemRepresentation> {

	private Long id;
	private String type;
	private String filename;
	private String format;
	private String hash;
	private Instant creation;
	private Instant modification;
	private Instant archiveTime;
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

	public Instant getCreation() {
		return creation;
	}

	public ZonedDateTime getCreationAsZoned(String zoneId) {
		return creation.atZone(ZoneId.of(zoneId));
	}

	public void setCreation(Instant creation) {
		this.creation = creation;
	}

	public Instant getModification() {
		return modification;
	}

	public ZonedDateTime getModificationAsZoned(String zoneId) {
		return modification.atZone(ZoneId.of(zoneId));
	}
	
	public void setModification(Instant modification) {
		this.modification = modification;
	}

	public Instant getArchiveTime() {
		return archiveTime;
	}

	public ZonedDateTime getArchiveTimeAsZoned(String zoneId) {
		return archiveTime.atZone(ZoneId.of(zoneId));
	}

	public void setArchiveTime(Instant archiveTime) {
		this.archiveTime = archiveTime;
	}

	public SortedSet<TagEntityRepresentation> getTags() {
		return tags;
	}

	public void setTags(SortedSet<TagEntityRepresentation> tags) {
		this.tags = tags;
	}

}
