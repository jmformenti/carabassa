package org.atypical.carabassa.indexer.rdbms.entity.converter;

import org.atypical.carabassa.core.model.enums.ItemType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ItemTypeConverter implements AttributeConverter<ItemType, String> {

    @Override
    public String convertToDatabaseColumn(ItemType entity) {
        if (entity == null) {
            return null;
        }
        return entity.getCode();
    }

    @Override
    public ItemType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        return ItemType.fromCode(code);
    }

}
