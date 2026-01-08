package org.atypical.carabassa.core.model;

import java.time.Instant;
import java.util.Set;

public interface Dataset {

    Long getId();

    void setId(Long id);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    Instant getCreation();

    void setCreation(Instant creation);

    Instant getModification();

    void setModification(Instant modification);

    Set<IndexedItem> getItems();

    void setItems(Set<IndexedItem> items);

}
