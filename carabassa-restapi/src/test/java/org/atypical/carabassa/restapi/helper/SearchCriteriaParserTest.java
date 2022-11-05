package org.atypical.carabassa.restapi.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.text.ParseException;
import java.time.Instant;

import org.apache.commons.lang3.time.DateUtils;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.PeriodType;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.junit.jupiter.api.Test;

class SearchCriteriaParserTest {

	@Test
	void test() {
		SearchCriteria searchCriteria = SearchCriteriaParser.parse("att:test test2 att2: 123");

		assertFalse(searchCriteria.isEmpty());
		assertEquals(3, searchCriteria.getConditions().size());
		assertEquals("att", searchCriteria.getConditions().get(0).getKey());
		assertEquals(SearchOperator.EQUAL, searchCriteria.getConditions().get(0).getOperation());
		assertEquals("test", searchCriteria.getConditions().get(0).getValue());
		assertEquals("test2", searchCriteria.getConditions().get(1).getValue());
		assertEquals("att2", searchCriteria.getConditions().get(2).getKey());
		assertEquals(SearchOperator.EQUAL, searchCriteria.getConditions().get(2).getOperation());
		assertEquals("123", searchCriteria.getConditions().get(2).getValue());
	}

	@Test
	void test2() throws ParseException {
		System.out.println(DateUtils.parseDateStrictly("2005", "yyyy", "yyyy-MM", "yyyy-MM-dd"));
		System.out.println(DateUtils.parseDateStrictly("2005-08", "yyyy", "yyyy-MM", "yyyy-MM-dd"));
		System.out.println(DateUtils.parseDateStrictly("2005-08-5", "yyyy", "yyyy-MM", "yyyy-MM-dd"));
	}

	@Test
	void test3() throws ParseException {
		Instant now = Instant.now();
		System.out.println(PeriodType.DAY.getPeriodDates(now));
		System.out.println(PeriodType.MONTH.getPeriodDates(now));
		System.out.println(PeriodType.YEAR.getPeriodDates(now));
	}

}
