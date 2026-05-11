package org.atypical.carabassa.core.search;

import org.apache.commons.lang3.StringUtils;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.atypical.carabassa.core.model.impl.SearchConditionImpl;
import org.atypical.carabassa.core.model.impl.SearchCriteriaImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchCriteriaParser {

    private static final String SEARCH_REGEX = "(?:\"([^\"]*)\"|([^:<>=\\s]+))(?:\\s*(>=|<=|:|<|>)\\s*(?:\"([^\"]*)\"|([^\\s]+)))?";

    private static final int KEY_QUOTED_POS = 1;
    private static final int KEY_UNQUOTED_POS = 2;
    private static final int OPERATOR_POS = 3;
    private static final int VALUE_QUOTED_POS = 4;
    private static final int VALUE_UNQUOTED_POS = 5;

    public static SearchCriteria parse(String search) {
        if (StringUtils.isBlank(search)) {
            return new SearchCriteriaImpl();
        }

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

            String keyOrValue = matcher.group(KEY_QUOTED_POS) != null ? matcher.group(KEY_QUOTED_POS) : matcher.group(KEY_UNQUOTED_POS);
            String operatorStr = matcher.group(OPERATOR_POS);

            if (operatorStr == null) {
                // Generic search (value only)
                searchCriteria.add(new SearchConditionImpl(keyOrValue));
            } else {
                if (StringUtils.isBlank(keyOrValue)) {
                    throw new IllegalArgumentException("Search key cannot be empty");
                }
                SearchOperator operator = SearchOperator.fromCode(operatorStr);
                if (operator == null) {
                    throw new IllegalArgumentException("Unsupported search operator: " + operatorStr);
                }
                String value = matcher.group(VALUE_QUOTED_POS) != null ? matcher.group(VALUE_QUOTED_POS) : matcher.group(VALUE_UNQUOTED_POS);

                // For tagged search, we normalize the key to lowercase
                searchCriteria.add(new SearchConditionImpl(keyOrValue.trim().toLowerCase(java.util.Locale.ROOT), operator, value));
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
