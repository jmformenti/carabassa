package org.atypical.carabassa.core.model;

import org.atypical.carabassa.core.model.enums.ValueType;

public interface TagInfo {

    Long getId();

    void setId(Long id);

    String getTagName();

    void setTagName(String tagName);

    String getDescription();

    void setDescription(String description);

    String getAlias();

    void setAlias(String alias);

    Boolean getInternal();

    void setInternal(Boolean internal);

    Boolean getSortable();

    void setSortable(Boolean sortable);

    ValueType getType();

    void setType(ValueType type);

    Boolean getShowInHelp();

    void setShowInHelp(Boolean showInHelp);
}
