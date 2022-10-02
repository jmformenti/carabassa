package org.atypical.carabassa.core.model.enums;

import java.time.ZonedDateTime;

import org.springframework.data.util.Pair;

public enum PeriodType {
	DAY, MONTH, YEAR;

	public Pair<ZonedDateTime, ZonedDateTime> getPeriodDates(ZonedDateTime startDate) {
		ZonedDateTime startOfDay = startDate.toLocalDate().atStartOfDay(startDate.getZone());
		switch (this) {
		case DAY:
			return Pair.of(startOfDay, startOfDay.plusDays(1).minusSeconds(1));
		case MONTH:
			return Pair.of(startOfDay, startOfDay.plusMonths(1).minusSeconds(1));
		case YEAR:
			return Pair.of(startOfDay, startOfDay.plusYears(1).minusSeconds(1));
		default:
			throw new IllegalArgumentException("Unknown period type.");
		}
	}
}
