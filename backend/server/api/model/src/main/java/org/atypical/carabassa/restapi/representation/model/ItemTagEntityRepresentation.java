package org.atypical.carabassa.restapi.representation.model;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "itemTagEntityRepresentationList")
public class ItemTagEntityRepresentation extends RepresentationModel<ItemTagEntityRepresentation> {

    private Long itemId;
    private Long tagId;
    private String tagName;
    private Object tagValue;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Object getTagValue() {
        return tagValue;
    }

    public void setTagValue(Object tagValue) {
        this.tagValue = tagValue;
    }

}
