package org.atypical.carabassa.restapi.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.atypical.carabassa.core.model.impl.SearchConditionImpl;
import org.atypical.carabassa.core.model.impl.SearchCriteriaImpl;

public class SearchCriteriaParser {

	private static final String SEARCH_REGEX = "([^:\\s]+)((:)\\s*([^\\s]+))?";

	private static final int ATTR_POS = 1;
	private static final int ONLY_VALUE_POS = 1;
	private static final int VALUE_GROUP_POS = 2;
	private static final int OPERATOR_POS = 3;
	private static final int VALUE_POS = 4;

	public static SearchCriteria parse(String search) {
		SearchCriteria searchCriteria = new SearchCriteriaImpl();
		Pattern pattern = Pattern.compile(SEARCH_REGEX);
		Matcher matcher = pattern.matcher(search);
		while (matcher.find()) {
			if (matcher.group(VALUE_GROUP_POS) == null) {
				searchCriteria.add(new SearchConditionImpl(matcher.group(ONLY_VALUE_POS)));
			} else {
				searchCriteria.add(new SearchConditionImpl(matcher.group(ATTR_POS).trim().toLowerCase(),
						SearchOperator.fromCode(matcher.group(OPERATOR_POS)), matcher.group(VALUE_POS).trim()));
			}
		}
		return searchCriteria;
	}
}
