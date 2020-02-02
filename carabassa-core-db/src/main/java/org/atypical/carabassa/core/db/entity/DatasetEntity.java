package org.atypical.carabassa.core.db.entity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;

@Entity
@Table(name = "DATASET")
@SequenceGenerator(initialValue = 1, name = "dataset_id_gen", sequenceName = "dataset_sequence")
public class DatasetEntity implements Dataset {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "dataset_id_gen")
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private String name;

	@Column(length = 255)
	private String description;

	@Column(nullable = false)
	private ZonedDateTime creation;

	private ZonedDateTime modification;

	@OneToMany(targetEntity = IndexedImageEntity.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "DATASET_ID")
	private Set<IndexedImage> images;

	public DatasetEntity() {
		super();
		this.images = new HashSet<>();
	}

	public DatasetEntity(String name) {
		this();
		this.name = name;
	}

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
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
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
	public Set<IndexedImage> getImages() {
		return images;
	}

	@Override
	public void setImages(Set<IndexedImage> images) {
		this.images = images;
	}

}
