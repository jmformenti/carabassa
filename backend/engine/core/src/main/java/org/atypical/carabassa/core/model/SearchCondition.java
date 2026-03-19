package org.atypical.carabassa.core.model;

import org.atypical.carabassa.core.model.enums.SearchOperator;

public interface SearchCondition {

    String getKey();

    void setKey(String key);

    SearchOperator getOperation();

    void setOperation(SearchOperator operation);

    Object getValue();

    void setValue(Object value);

}
