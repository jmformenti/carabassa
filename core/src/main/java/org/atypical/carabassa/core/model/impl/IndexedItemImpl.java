package org.atypical.carabassa.core.model.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;

public class IndexedItemImpl implements IndexedItem {

	private Long id;
	private ItemType type;
	private String filename;
	private String format;
	private String hash;
	private Instant creation;
	private Instant modification;
	private Instant archiveTime;
	private long size;
	private Set<Tag> tags;
	private Dataset dataset;

	public IndexedItemImpl() {
		super();
	}

	public IndexedItemImpl(IndexedItem item) {
		super();
		this.id = item.getId();
		this.type = item.getType();
		this.filename = item.getFilename();
		this.format = item.getFormat();
		this.hash = item.getHash();
		this.creation = item.getCreation();
		this.modification = item.getModification();
		this.archiveTime = item.getArchiveTime();
		this.tags = item.getTags().stream().collect(Collectors.toSet());
		this.dataset = item.getDataset();
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
	public ItemType getType() {
		return type;
	}

	@Override
	public void setType(ItemType type) {
		this.type = type;
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
	public String getFormat() {
		return format;
	}

	@Override
	public void setFormat(String format) {
		this.format = format;
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
	public Instant getCreation() {
		return creation;
	}

	@Override
	public void setCreation(Instant creation) {
		this.creation = creation;
	}

	@Override
	public Instant getModification() {
		return modification;
	}

	@Override
	public void setModification(Instant modification) {
		this.modification = modification;
	}

	@Override
	public Instant getArchiveTime() {
		return archiveTime;
	}

	@Override
	public ZonedDateTime getArchiveTimeAsZoned(String zoneId) {
		return archiveTime.atZone(ZoneId.of(zoneId));
	}

	@Override
	public void setArchiveTime(Instant archiveTime) {
		this.archiveTime = archiveTime;
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public void setSize(long size) {
		this.size = size;
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
