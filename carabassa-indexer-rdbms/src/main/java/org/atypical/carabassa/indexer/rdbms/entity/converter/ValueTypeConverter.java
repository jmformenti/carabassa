package org.atypical.carabassa.indexer.rdbms.entity.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.atypical.carabassa.indexer.rdbms.entity.enums.ValueType;

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
