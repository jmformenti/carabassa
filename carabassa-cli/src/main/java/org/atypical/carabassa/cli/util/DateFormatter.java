package org.atypical.carabassa.cli.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormatter {

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	public static String toLocalDateFormatted(ZonedDateTime zonedDateTime) {
		return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(formatter);
	}
}
