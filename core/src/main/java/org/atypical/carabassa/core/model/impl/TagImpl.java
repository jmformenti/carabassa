package org.atypical.carabassa.core.model.impl;

import java.time.Instant;

import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.core.model.Tag;

public class TagImpl implements Tag {

    private Long id;
    private String name;
    private Instant creation;
    private Object value;
    private BoundingBox boundingBox;

    public TagImpl(String name, Object value) {
        super();
        this.name = name;
        this.value = value;
    }

    public TagImpl(Tag tag) {
        super();
        this.id = tag.getId();
        this.name = tag.getName();
        this.setValue(tag.getValue());
        this.boundingBox = tag.getBoundingBox();
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
    public Instant getCreation() {
        return creation;
    }

    public void setCreation(Instant creation) {
        this.creation = creation;
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
    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Class<T> clazz) {
        return (T) value;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    @Override
    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

}
