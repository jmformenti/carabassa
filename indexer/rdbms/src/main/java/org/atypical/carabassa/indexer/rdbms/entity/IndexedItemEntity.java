package org.atypical.carabassa.indexer.rdbms.entity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.indexer.rdbms.entity.converter.ItemTypeConverter;

@Entity
@Table(name = "ITEM")
@SequenceGenerator(name = "item_id_gen", sequenceName = "item_sequence")
public class IndexedItemEntity implements IndexedItem {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "item_id_gen")
	private Long id;

	@Column(length = 1)
	@Convert(converter = ItemTypeConverter.class)
	private ItemType type;

	@Column(nullable = false)
	private String filename;

	@Column(length = 10)
	private String format;

	@Column(unique = true, nullable = false)
	private String hash;

	@Column(nullable = false)
	private Instant creation;

	private Instant modification;

	private Instant archiveTime;

	private long size;

	@OneToMany(targetEntity = TagEntity.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ITEM_ID")
	private Set<Tag> tags;

	@ManyToOne(targetEntity = DatasetEntity.class)
	private Dataset dataset;

	@PrePersist
	public void onPrePersist() {
		this.creation = Instant.now();
	}

	@PreUpdate
	public void onPreUpdate() {
		this.modification = Instant.now();
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filename == null) ? 0 : filename.hashCode());
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexedItemEntity other = (IndexedItemEntity) obj;
		if (filename == null) {
			if (other.filename != null)
				return false;
		} else if (!filename.equals(other.filename))
			return false;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		return true;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
