package org.atypical.carabassa.core.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.atypical.carabassa.core.model.SearchCondition;
import org.atypical.carabassa.core.model.SearchCriteria;

public class SearchCriteriaImpl implements SearchCriteria {

	private List<SearchCondition> conditions;

	public SearchCriteriaImpl() {
		this.conditions = new ArrayList<>();
	}

	public SearchCriteriaImpl(SearchCondition searchCondition) {
		this();
		this.conditions.add(searchCondition);
	}

	public SearchCriteriaImpl(List<SearchCondition> conditions) {
		this.conditions = conditions;
	}

	@Override
	public List<SearchCondition> getConditions() {
		return conditions;
	}

	@Override
	public void add(SearchCondition searchCondition) {
		this.conditions.add(searchCondition);
	}

	@Override
	public boolean isEmpty() {
		return this.conditions.isEmpty();
	}

}
