package org.atypical.carabassa.core.model;

import java.util.List;

public interface SearchCriteria {

	List<SearchCondition> getConditions();

	void add(SearchCondition searchCondition);

	boolean isEmpty();

}
