package org.atypical.carabassa.restapi.helper;

import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.atypical.carabassa.core.model.impl.SearchConditionImpl;
import org.atypical.carabassa.core.model.impl.SearchCriteriaImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchCriteriaParser {

    private static final String SEARCH_REGEX = "([^:<>=\\s]+)((>=|<=|:|<|>)\\s*([^\\s]+))?";

    private static final int ATTR_POS = 1;
    private static final int ONLY_VALUE_POS = 1;
    private static final int VALUE_GROUP_POS = 2;
    private static final int OPERATOR_POS = 3;
    private static final int VALUE_POS = 4;

    public static SearchCriteria parse(String search) {
        SearchCriteria searchCriteria = new SearchCriteriaImpl();
        Pattern pattern = Pattern.compile(SEARCH_REGEX);
        Matcher matcher = pattern.matcher(search);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String skipped = search.substring(lastEnd, matcher.start()).trim();
                if (!skipped.isEmpty()) {
                    throw new IllegalArgumentException("Invalid search expression at: " + skipped);
                }
            }
            lastEnd = matcher.end();
            
            if (matcher.group(VALUE_GROUP_POS) == null) {
                searchCriteria.add(new SearchConditionImpl(matcher.group(ONLY_VALUE_POS)));
            } else {
                SearchOperator operator = SearchOperator.fromCode(matcher.group(OPERATOR_POS));
                // Fallback validation for operator, although regex might restrict it
                if (operator == null) {
                    throw new IllegalArgumentException("Unsupported search operator: " + matcher.group(OPERATOR_POS));
                }
                searchCriteria.add(new SearchConditionImpl(matcher.group(ATTR_POS).trim().toLowerCase(),
                        operator, matcher.group(VALUE_POS).trim()));
            }
        }
        
        if (lastEnd < search.length()) {
            String skipped = search.substring(lastEnd).trim();
            if (!skipped.isEmpty()) {
                throw new IllegalArgumentException("Invalid search expression at: " + skipped);
            }
        }
        
        return searchCriteria;
    }
}
