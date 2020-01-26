package org.atypical.core.db.entity.converter;

import java.io.IOException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.atypical.core.model.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Converter
public class BoundingBoxConverter implements AttributeConverter<BoundingBox, String> {

	private static final Logger logger = LoggerFactory.getLogger(BoundingBoxConverter.class);

	private final static ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(BoundingBox bb) {
		try {
			return objectMapper.writeValueAsString(bb);
		} catch (JsonProcessingException ex) {
			logger.error("Error converting to json", ex);
			return null;
		}
	}

	@Override
	public BoundingBox convertToEntityAttribute(String bb) {
		try {
			return objectMapper.readValue(bb, BoundingBox.class);
		} catch (IOException ex) {
			logger.error("Error converting to bb", ex);
			return null;
		}
	}
}