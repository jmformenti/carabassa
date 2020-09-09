package org.atypical.carabassa.core.db.entity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.atypical.carabassa.core.db.entity.converter.ValueTypeConverter;
import org.atypical.carabassa.core.db.entity.enums.ValueType;
import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.core.model.Tag;

@Entity
@Table(name = "TAG")
@SequenceGenerator(initialValue = 1, name = "tag_id_gen", sequenceName = "tag_sequence")
public class TagEntity implements Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "tag_id_gen")
	private Long id;

	@Column(nullable = false)
	private String name;
	private ZonedDateTime creation;

	@Column(length = 1)
	@Convert(converter = ValueTypeConverter.class)
	private ValueType valueType;

	@Column(length = 255)
	private String textValue;
	private ZonedDateTime dateValue;
	private Long longValue;
	private Double doubleValue;
	private Boolean booleanValue;

	@OneToOne(targetEntity = BoundingBoxEntity.class)
	private BoundingBox boundingBox;

	public TagEntity() {
		super();
	}

	public TagEntity(Tag tag) {
		super();
		this.id = tag.getId();
		this.name = tag.getName();
		this.setValue(tag.getValue());
		this.boundingBox = tag.getBoundingBox();
	}

	@PrePersist
	public void onPrePersist() {
		this.creation = ZonedDateTime.now();
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
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ZonedDateTime getCreation() {
		return creation;
	}

	public ValueType getValueType() {
		return valueType;
	}

	public String getTextValue() {
		return textValue;
	}

	public ZonedDateTime getDateValue() {
		return dateValue;
	}

	public Long getLongValue() {
		return longValue;
	}

	public Double getDoubleValue() {
		return doubleValue;
	}

	public Boolean getBooleanValue() {
		return booleanValue;
	}

	@Override
	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}

	@Override
	public Object getValue() {
		switch (valueType) {
		case STRING:
			return this.textValue;
		case LONG:
			return this.longValue;
		case DOUBLE:
			return this.doubleValue;
		case BOOLEAN:
			return this.booleanValue;
		case DATE:
			return this.dateValue;
		default:
			return this.textValue;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(Class<T> clazz) {
		return (T) getValue();
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof String) {
			this.valueType = ValueType.STRING;
			this.textValue = (String) value;
		} else if (value instanceof byte[]) {
			this.valueType = ValueType.STRING;
			this.textValue = Arrays.toString((byte[]) value);
		} else if (value instanceof Long) {
			this.valueType = ValueType.LONG;
			this.longValue = (Long) value;
		} else if (value instanceof Integer) {
			this.valueType = ValueType.LONG;
			this.longValue = ((Integer) value).longValue();
		} else if (value instanceof Double) {
			this.valueType = ValueType.DOUBLE;
			this.doubleValue = (Double) value;
		} else if (value instanceof Float) {
			this.valueType = ValueType.DOUBLE;
			this.doubleValue = ((Float) value).doubleValue();
		} else if (value instanceof Boolean) {
			this.valueType = ValueType.BOOLEAN;
			this.booleanValue = (Boolean) value;
		} else if (value instanceof ZonedDateTime) {
			this.valueType = ValueType.DATE;
			this.dateValue = (ZonedDateTime) value;
		} else if (value instanceof LocalDate) {
			this.valueType = ValueType.DATE;
			this.dateValue = ((LocalDate) value).atStartOfDay(ZoneId.systemDefault());
		} else if (value instanceof Date) {
			this.valueType = ValueType.DATE;
			this.dateValue = ((Date) value).toInstant().atZone(ZoneId.systemDefault());
		} else {
			// if not supported type save as string
			this.valueType = ValueType.STRING;
			this.textValue = value.toString();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((booleanValue == null) ? 0 : booleanValue.hashCode());
		result = prime * result + ((boundingBox == null) ? 0 : boundingBox.hashCode());
		result = prime * result + ((dateValue == null) ? 0 : dateValue.hashCode());
		result = prime * result + ((doubleValue == null) ? 0 : doubleValue.hashCode());
		result = prime * result + ((longValue == null) ? 0 : longValue.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((textValue == null) ? 0 : textValue.hashCode());
		result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TagEntity other = (TagEntity) obj;
		if (booleanValue == null) {
			if (other.booleanValue != null)
				return false;
		} else if (!booleanValue.equals(other.booleanValue))
			return false;
		if (boundingBox == null) {
			if (other.boundingBox != null)
				return false;
		} else if (!boundingBox.equals(other.boundingBox))
			return false;
		if (dateValue == null) {
			if (other.dateValue != null)
				return false;
		} else if (!dateValue.equals(other.dateValue))
			return false;
		if (doubleValue == null) {
			if (other.doubleValue != null)
				return false;
		} else if (!doubleValue.equals(other.doubleValue))
			return false;
		if (longValue == null) {
			if (other.longValue != null)
				return false;
		} else if (!longValue.equals(other.longValue))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (textValue == null) {
			if (other.textValue != null)
				return false;
		} else if (!textValue.equals(other.textValue))
			return false;
		if (valueType != other.valueType)
			return false;
		return true;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
