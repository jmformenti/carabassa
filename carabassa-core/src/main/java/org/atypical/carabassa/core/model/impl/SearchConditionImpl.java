package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.SearchCondition;
import org.atypical.carabassa.core.model.enums.SearchOperator;

public class SearchConditionImpl implements SearchCondition {

	private String key;
	private SearchOperator operation;
	private Object value;

	public SearchConditionImpl(Object value) {
		super();
		this.value = value;
	}

	public SearchConditionImpl(String key, SearchOperator operation, Object value) {
		super();
		this.key = key;
		this.operation = operation;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public SearchOperator getOperation() {
		return operation;
	}

	public void setOperation(SearchOperator operation) {
		this.operation = operation;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}
