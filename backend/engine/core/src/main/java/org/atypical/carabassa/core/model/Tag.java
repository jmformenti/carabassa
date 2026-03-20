package org.atypical.carabassa.core.model;

import java.time.Instant;

public interface Tag {

    Long getId();

    void setId(Long id);

    Instant getCreation();

    String getName();

    void setName(String name);

    Object getValue();

    <T> T getValue(Class<T> clazz);

    void setValue(Object value);

    BoundingBox getBoundingBox();

    void setBoundingBox(BoundingBox boundingBox);

}
