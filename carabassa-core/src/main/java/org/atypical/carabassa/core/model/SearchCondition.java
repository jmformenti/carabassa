package org.atypical.carabassa.core.model;

import org.atypical.carabassa.core.model.enums.SearchOperator;

public interface SearchCondition {

	public String getKey();

	public void setKey(String key);

	public SearchOperator getOperation();

	public void setOperation(SearchOperator operation);

	public Object getValue();

	public void setValue(Object value);

}
