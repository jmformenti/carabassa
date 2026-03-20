package org.atypical.carabassa.core.model.enums;

import org.springframework.data.util.Pair;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum PeriodType {
    DAY, MONTH, YEAR;

    public Pair<Instant, Instant> getPeriodDates(Instant startDate) {
        ZonedDateTime startOfDay = LocalDate.ofInstant(startDate, ZoneId.of("UTC")).atStartOfDay(ZoneId.of("UTC"));
        switch (this) {
            case DAY:
                return Pair.of(startOfDay.toInstant(), startOfDay.plusDays(1).minusSeconds(1).toInstant());
            case MONTH:
                return Pair.of(startOfDay.toInstant(), startOfDay.plusMonths(1).minusSeconds(1).toInstant());
            case YEAR:
                return Pair.of(startOfDay.toInstant(), startOfDay.plusYears(1).minusSeconds(1).toInstant());
            default:
                throw new IllegalArgumentException("Unknown period type.");
        }
    }
}
