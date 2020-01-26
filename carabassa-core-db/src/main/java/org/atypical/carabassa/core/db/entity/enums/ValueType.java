package org.atypical.carabassa.core.db.entity.enums;

import java.util.HashMap;
import java.util.Map;

public enum ValueType {
	STRING("S"), LONG("L"), DOUBLE("F"), DATE("D"), BOOLEAN("B");

	private String code;

	private static final Map<String, ValueType> codes = new HashMap<>();

	static {
		for (ValueType resolution : ValueType.values()) {
			codes.put(resolution.getCode(), resolution);
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

	@Override
	public String toString() {
		return code;
	}

}
