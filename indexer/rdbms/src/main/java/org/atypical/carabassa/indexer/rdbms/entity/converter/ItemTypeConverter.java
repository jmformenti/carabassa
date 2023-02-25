package org.atypical.carabassa.indexer.rdbms.entity.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.atypical.carabassa.core.model.enums.ItemType;

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
