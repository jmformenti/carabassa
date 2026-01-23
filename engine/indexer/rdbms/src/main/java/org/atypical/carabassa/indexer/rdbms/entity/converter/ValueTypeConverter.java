package org.atypical.carabassa.indexer.rdbms.entity.converter;

import org.atypical.carabassa.indexer.rdbms.entity.enums.ValueType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ValueTypeConverter implements AttributeConverter<ValueType, String> {

    @Override
    public String convertToDatabaseColumn(ValueType entity) {
        if (entity == null) {
            return null;
        }
        return entity.getCode();
    }

    @Override
    public ValueType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        return ValueType.fromCode(code);
    }

}
