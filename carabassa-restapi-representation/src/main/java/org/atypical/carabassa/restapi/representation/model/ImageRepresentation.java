package org.atypical.carabassa.restapi.representation.model;

import java.time.ZonedDateTime;
import java.util.SortedSet;

import org.springframework.hateoas.RepresentationModel;

public class ImageRepresentation extends RepresentationModel<ImageRepresentation> {

	private Long id;
	private String filename;
	private String fileType;
	private String hash;
	private ZonedDateTime creation;
	private ZonedDateTime modification;
	private ZonedDateTime archiveTime;
	private SortedSet<TagRepresentation> tags;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
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

	public SortedSet<TagRepresentation> getTags() {
		return tags;
	}

	public void setTags(SortedSet<TagRepresentation> tags) {
		this.tags = tags;
	}

}
