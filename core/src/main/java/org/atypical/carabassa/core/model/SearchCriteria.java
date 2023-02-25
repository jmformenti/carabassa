package org.atypical.carabassa.core.model;

import java.util.List;

public interface SearchCriteria {

	public List<SearchCondition> getConditions();

	public void add(SearchCondition searchCondition);

	public boolean isEmpty();

}
