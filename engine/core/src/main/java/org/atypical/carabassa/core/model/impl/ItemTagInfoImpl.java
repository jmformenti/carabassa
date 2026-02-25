package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.ItemTagInfo;
import org.atypical.carabassa.core.model.Tag;

public class ItemTagInfoImpl implements ItemTagInfo {

    private final Long itemId;
    private final Tag tag;

    public ItemTagInfoImpl(Long itemId, Tag tag) {
        this.itemId = itemId;
        this.tag = tag;
    }

    @Override
    public Long getItemId() {
        return itemId;
    }

    @Override
    public Tag getTag() {
        return tag;
    }
}
