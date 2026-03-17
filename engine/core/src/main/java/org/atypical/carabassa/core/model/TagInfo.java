package org.atypical.carabassa.core.model;

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
}
