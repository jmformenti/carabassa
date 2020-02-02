package org.atypical.carabassa.core.model.impl;

import java.time.ZonedDateTime;
import java.util.Set;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.Tag;

public class IndexedImageImpl implements IndexedImage {

	private Long id;
	private String filename;
	private String fileType;
	private String hash;
	private ZonedDateTime creation;
	private ZonedDateTime modification;
	private ZonedDateTime archiveTime;
	private Set<Tag> tags;
	private Dataset dataset;

	public IndexedImageImpl() {
		super();
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
	public String getFilename() {
		return filename;
	}

	@Override
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public String getFileType() {
		return fileType;
	}

	@Override
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	@Override
	public String getHash() {
		return hash;
	}

	@Override
	public void setHash(String hash) {
		this.hash = hash;
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
	public ZonedDateTime getArchiveTime() {
		return archiveTime;
	}

	@Override
	public void setArchiveTime(ZonedDateTime archiveTime) {
		this.archiveTime = archiveTime;
	}

	@Override
	public Set<Tag> getTags() {
		return tags;
	}

	@Override
	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public Dataset getDataset() {
		return dataset;
	}

	@Override
	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

}
