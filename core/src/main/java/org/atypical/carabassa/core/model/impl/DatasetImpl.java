package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class DatasetImpl implements Dataset {

    private Long id;
    private String name;
    private String description;
    private Instant creation;
    private Instant modification;
    private Set<IndexedItem> items;

    public DatasetImpl() {
        super();
        this.items = new HashSet<>();
    }

    public DatasetImpl(String name) {
        this();
        this.name = name;
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
