package org.atypical.carabassa.indexer.rdbms.entity.enums;

import java.util.HashMap;
import java.util.Map;

public enum ValueType {
	STRING("S"), LONG("L"), DOUBLE("F"), DATE("D"), BOOLEAN("B");

	private String code;

	private static final Map<String, ValueType> codes = new HashMap<>();

	static {
		for (ValueType valueType : ValueType.values()) {
			codes.put(valueType.getCode(), valueType);
		}
	}

	private ValueType(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static ValueType fromCode(String code) {
		return codes.get(code);
	}

}
