package org.atypical.carabassa.restapi.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.time.DateUtils;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.PeriodType;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

class SearchCriteriaParserTest {

	@Test
	void testCriteriaParser() {
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
	void testDatesCompletion() throws ParseException {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		assertEquals("2005-01-01",
				formatter.format(DateUtils.parseDateStrictly("2005", "yyyy", "yyyy-MM", "yyyy-MM-dd")));
		assertEquals("2005-08-01",
				formatter.format(DateUtils.parseDateStrictly("2005-08", "yyyy", "yyyy-MM", "yyyy-MM-dd")));
		assertEquals("2005-08-05",
				formatter.format(DateUtils.parseDateStrictly("2005-08-05", "yyyy", "yyyy-MM", "yyyy-MM-dd")));
	}

	@Test
	void testPeriodTypes() throws ParseException {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

		Instant instant = LocalDateTime.of(2005, 1, 1, 11, 11).toInstant(ZoneOffset.UTC);
		
		Pair<Instant, Instant> period = PeriodType.DAY.getPeriodDates(instant);
		assertEquals("2005-01-01 00:00:00", formatter.format(period.getFirst()));
		assertEquals("2005-01-01 23:59:59", formatter.format(period.getSecond()));

		period = PeriodType.MONTH.getPeriodDates(instant);
		assertEquals("2005-01-01 00:00:00", formatter.format(period.getFirst()));
		assertEquals("2005-01-31 23:59:59", formatter.format(period.getSecond()));

		period = PeriodType.YEAR.getPeriodDates(instant);
		assertEquals("2005-01-01 00:00:00", formatter.format(period.getFirst()));
		assertEquals("2005-12-31 23:59:59", formatter.format(period.getSecond()));
	}

}
