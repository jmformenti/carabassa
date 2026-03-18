package org.atypical.carabassa.indexer.rdbms.entity;

import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.core.model.enums.ValueType;
import org.atypical.carabassa.indexer.rdbms.entity.converter.ValueTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "TAG_INFO")
@SequenceGenerator(name = "tag_info_id_gen", sequenceName = "tag_info_sequence")
public class TagInfoEntity implements TagInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "tag_info_id_gen")
    private Long id;

    @Column(name = "TAG_NAME", nullable = false, unique = true)
    private String tagName;

    @Column
    private String description;

    @Column(unique = true)
    private String alias;

    @Column(nullable = false)
    private Boolean internal = false;

    @Column(nullable = false)
    private Boolean sortable = false;

    @Column(name = "VALUE_TYPE", length = 1, nullable = false)
    @Convert(converter = ValueTypeConverter.class)
    private ValueType type = ValueType.STRING;

    public TagInfoEntity() {
        super();
    }

    public TagInfoEntity(TagInfo tagInfo) {
        this.id = tagInfo.getId();
        this.tagName = tagInfo.getTagName();
        this.description = tagInfo.getDescription();
        this.alias = tagInfo.getAlias();
        this.internal = tagInfo.getInternal();
        this.sortable = tagInfo.getSortable();
        this.type = tagInfo.getType();
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
    public String getTagName() {
        return tagName;
    }

    @Override
    public void setTagName(String tagName) {
        this.tagName = tagName;
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
    public String getAlias() {
        return alias;
    }

    @Override
    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public Boolean getInternal() {
        return internal;
    }

    @Override
    public void setInternal(Boolean internal) {
        this.internal = internal;
    }

    @Override
    public Boolean getSortable() {
        return sortable;
    }

    @Override
    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public void setType(ValueType type) {
        this.type = type;
    }
}
