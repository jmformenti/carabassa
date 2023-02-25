package org.atypical.carabassa.core.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.atypical.carabassa.core.model.enums.ItemType;

public interface IndexedItem {

    Long getId();

    void setId(Long id);

    ItemType getType();

    void setType(ItemType type);

    String getFilename();

    void setFilename(String filename);

    String getFormat();

    void setFormat(String format);

    String getHash();

    void setHash(String hash);

    Instant getCreation();

    void setCreation(Instant creation);

    Instant getModification();

    void setModification(Instant modification);

    Instant getArchiveTime();

    ZonedDateTime getArchiveTimeAsZoned(String zoneId);

    void setArchiveTime(Instant archiveTime);

    long getSize();

    void setSize(long size);

    Set<Tag> getTags();

    void setTags(Set<Tag> tags);

    default Set<Tag> getTags(String name) {
        if (name != null) {
            return getTags().stream().filter(t -> name.equals(t.getName())).collect(Collectors.toSet());
        } else {
            return null;
        }
    }

    default Tag getFirstTag(String name) {
        if (name != null) {
            return getTags().stream().filter(t -> name.equals(t.getName())).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    default boolean isArchived() {
        return getArchiveTime() != null;
    }

    Dataset getDataset();

    void setDataset(Dataset dataset);

}
