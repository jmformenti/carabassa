package org.atypical.carabassa.restapi.representation.model;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Relation(collectionRelation = "datasetEntityRepresentationList")
public class DatasetEntityRepresentation extends RepresentationModel<DatasetEntityRepresentation> {

    private Long id;
    private String name;
    private String description;
    private Instant creation;
    private Instant modification;

    public DatasetEntityRepresentation() {
        super();
    }

    public DatasetEntityRepresentation(String name) {
        super();
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreation() {
        return creation;
    }

    public ZonedDateTime getCreationAsZoned(String zoneId) {
        return creation.atZone(ZoneId.of(zoneId));
    }

    public void setCreation(Instant creation) {
        this.creation = creation;
    }

    public Instant getModification() {
        return modification;
    }

    public ZonedDateTime getModificationAsZoned(String zoneId) {
        return modification.atZone(ZoneId.of(zoneId));
    }

    public void setModification(Instant modification) {
        this.modification = modification;
    }
}
