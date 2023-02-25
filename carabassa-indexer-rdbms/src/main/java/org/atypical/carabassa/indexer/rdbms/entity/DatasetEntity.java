package org.atypical.carabassa.indexer.rdbms.entity;

import java.time.Instant;
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
import org.atypical.carabassa.core.model.IndexedItem;

@Entity
@Table(name = "DATASET")
@SequenceGenerator(initialValue = 1, name = "dataset_id_gen", sequenceName = "dataset_sequence")
public class DatasetEntity implements Dataset {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "dataset_id_gen")
	private Long id;

	@Column(unique = true, nullable = false)
	private String name;

	@Column(length = 255)
	private String description;

	@Column(nullable = false)
	private Instant creation;

	private Instant modification;

	@OneToMany(targetEntity = IndexedItemEntity.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "DATASET_ID")
	private Set<IndexedItem> items;

	public DatasetEntity() {
		super();
		this.items = new HashSet<>();
	}

	public DatasetEntity(String name) {
		this();
		this.name = name;
	}

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
	public Set<IndexedItem> getItems() {
		return items;
	}

	@Override
	public void setItems(Set<IndexedItem> items) {
		this.items = items;
	}

}
