package org.atypical.carabassa.indexer.rdbms.entity;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.atypical.carabassa.core.model.BoundingBox;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.enums.ValueType;
import org.atypical.carabassa.indexer.rdbms.entity.converter.ValueTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;

import jakarta.persistence.CascadeType;

@Entity
@Table(
        name = "TAG",
        indexes = {
                @Index(name = "idx_tag_name_item_id", columnList = "NAME, ITEM_ID")
        }
)
@SequenceGenerator(name = "tag_id_gen", sequenceName = "tag_sequence")
public class TagEntity implements Tag {

    private static final int MAX_TEXT_LENGTH = 255;

    private static final DecimalFormat df = new DecimalFormat("#,###.##");
    private static final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private static final String DATE_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$";
    private static final String TIMESTAMP_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*$";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "tag_id_gen")
    private Long id;

    @Column(nullable = false)
    private String name;
    private Instant creation;

    @Column(length = 1)
    @Convert(converter = ValueTypeConverter.class)
    private ValueType valueType;

    @Column(length = MAX_TEXT_LENGTH)
    private String textValue;
    private Instant dateValue;
    private Long longValue;
    private Double doubleValue;
    private Boolean booleanValue;

    @Column(name = "ITEM_ID", insertable = false, updatable = false)
    private Long itemId;

    @OneToOne(targetEntity = BoundingBoxEntity.class, cascade = CascadeType.ALL)
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
        this.creation = Instant.now();
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
    public Instant getCreation() {
        return creation;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public String getTextValue() {
        return textValue;
    }

    public Instant getDateValue() {
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

    public Long getItemId() {
        return itemId;
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
            String stringValue = (String) value;
            Instant parsedDate = parseDate(stringValue);
            if (parsedDate != null) {
                this.valueType = ValueType.DATE;
                this.dateValue = parsedDate;
                this.textValue = stringValue.matches(DATE_PATTERN) ? stringValue
                        : formatter.format(Date.from(this.dateValue));
                return;
            }
            this.valueType = ValueType.STRING;
            this.textValue = StringUtils.left(stringValue, MAX_TEXT_LENGTH);
        } else if (value instanceof byte[]) {
            this.valueType = ValueType.STRING;
            this.textValue = StringUtils.left(Arrays.toString((byte[]) value), MAX_TEXT_LENGTH);
        } else if (value instanceof Long) {
            this.valueType = ValueType.LONG;
            this.longValue = (Long) value;
            this.textValue = String.valueOf(this.longValue);
        } else if (value instanceof Integer) {
            this.valueType = ValueType.LONG;
            this.longValue = ((Integer) value).longValue();
            this.textValue = String.valueOf(this.longValue);
        } else if (value instanceof Double) {
            this.valueType = ValueType.DOUBLE;
            this.doubleValue = (Double) value;
            this.textValue = df.format(this.doubleValue);
        } else if (value instanceof Float) {
            this.valueType = ValueType.DOUBLE;
            this.doubleValue = ((Float) value).doubleValue();
            this.textValue = df.format(this.doubleValue);
        } else if (value instanceof Boolean) {
            this.valueType = ValueType.BOOLEAN;
            this.booleanValue = (Boolean) value;
            this.textValue = BooleanUtils.toStringTrueFalse(this.booleanValue);
        } else if (value instanceof Instant) {
            this.valueType = ValueType.DATE;
            this.dateValue = (Instant) value;
            this.textValue = formatter.format(Date.from(this.dateValue));
        } else if (value instanceof ZonedDateTime) {
            this.valueType = ValueType.DATE;
            this.dateValue = ((ZonedDateTime) value).toInstant();
            this.textValue = formatter.format(Date.from(this.dateValue));
        } else if (value instanceof LocalDate) {
            this.valueType = ValueType.DATE;
            this.dateValue = ((LocalDate) value).atStartOfDay(ZoneId.of("UTC")).toInstant();
            this.textValue = formatter.format(Date.from(this.dateValue));
        } else if (value instanceof Date) {
            this.valueType = ValueType.DATE;
            this.dateValue = ((Date) value).toInstant().atZone(ZoneId.of("UTC")).toInstant();
            this.textValue = formatter.format(Date.from(this.dateValue));
        } else {
            // if not supported type save as string
            this.valueType = ValueType.STRING;
            this.textValue = StringUtils.left(value.toString(), MAX_TEXT_LENGTH);
        }
    }

    public void setValue(Object value, ValueType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null for explicit type assignment.");
        }

        this.valueType = type;
        String stringValue = value != null ? String.valueOf(value) : null;
        switch (type) {
            case STRING:
                this.textValue = StringUtils.left(stringValue, MAX_TEXT_LENGTH);
                break;
            case LONG:
                if (value instanceof Number) {
                    this.longValue = ((Number) value).longValue();
                } else {
                    this.longValue = Long.valueOf(stringValue);
                }
                this.textValue = String.valueOf(this.longValue);
                break;
            case DOUBLE:
                if (value instanceof Number) {
                    this.doubleValue = ((Number) value).doubleValue();
                } else {
                    this.doubleValue = Double.valueOf(stringValue);
                }
                this.textValue = df.format(this.doubleValue);
                break;
            case BOOLEAN:
                if (value instanceof Boolean) {
                    this.booleanValue = (Boolean) value;
                } else {
                    this.booleanValue = BooleanUtils.toBoolean(stringValue);
                }
                this.textValue = BooleanUtils.toStringTrueFalse(this.booleanValue);
                break;
            case DATE:
                if (value instanceof Instant) {
                    this.dateValue = (Instant) value;
                } else if (value instanceof ZonedDateTime) {
                    this.dateValue = ((ZonedDateTime) value).toInstant();
                } else if (value instanceof LocalDate) {
                    this.dateValue = ((LocalDate) value).atStartOfDay(ZoneId.of("UTC")).toInstant();
                } else {
                    this.dateValue = parseDate(stringValue);
                }
                this.textValue = formatter.format(Date.from(this.dateValue));
                break;
            default:
                this.valueType = ValueType.STRING;
                this.textValue = StringUtils.left(stringValue, MAX_TEXT_LENGTH);
        }
    }

    private Instant parseDate(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        if (stringValue.matches(DATE_PATTERN)) {
            try {
                return LocalDate.parse(stringValue).atStartOfDay(ZoneId.of("UTC")).toInstant();
            } catch (Exception ignored) {
            }
        } else if (stringValue.matches(TIMESTAMP_PATTERN)) {
            try {
                return Instant.parse(stringValue);
            } catch (Exception ignored) {
            }
        }
        return null;
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
