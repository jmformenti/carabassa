package org.atypical.carabassa.core.model.enums;

import java.util.HashMap;
import java.util.Map;

public enum SearchOperator {

    EQUAL(":"), GREATER_THAN(">"), LESS_THAN("<");

    private static final Map<String, SearchOperator> codes = new HashMap<>();

    static {
        for (SearchOperator operator : SearchOperator.values()) {
            codes.put(operator.getCode(), operator);
        }
    }

    private final String code;

    SearchOperator(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SearchOperator fromCode(String code) {
        return codes.get(code);
    }

}
