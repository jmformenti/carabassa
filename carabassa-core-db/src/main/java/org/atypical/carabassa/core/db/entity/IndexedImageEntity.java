package org.atypical.carabassa.core.db.entity;

import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.Tag;

@Entity
@Table(name = "IMAGE")
@SequenceGenerator(initialValue = 1, name = "image_id_gen", sequenceName = "image_sequence")
public class IndexedImageEntity implements IndexedImage {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "image_id_gen")
	private Long id;

	@Column(nullable = false)
	private String filename;

	@Column(length = 10)
	private String fileType;

	@Column(unique = true, nullable = false)
	private String hash;

	@Column(nullable = false)
	private ZonedDateTime creation;

	private ZonedDateTime modification;

	private ZonedDateTime archiveTime;

	@OneToMany(targetEntity = TagEntity.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "IMAGE_ID")
	private Set<Tag> tags;

	@ManyToOne(targetEntity = DatasetEntity.class)
	private Dataset dataset;

	@PrePersist
	public void onPrePersist() {
		this.creation = ZonedDateTime.now();
	}

	@PreUpdate
	public void onPreUpdate() {
		this.modification = ZonedDateTime.now();
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
		IndexedImageEntity other = (IndexedImageEntity) obj;
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
