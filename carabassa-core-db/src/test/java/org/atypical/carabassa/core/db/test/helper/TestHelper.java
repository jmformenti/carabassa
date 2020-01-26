package org.atypical.carabassa.core.db.test.helper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Assertions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class TestHelper {

	public static Resource getImageResource(String filename) throws IOException {
		return new ClassPathResource("images/" + filename);
	}

	public static void assertDateInUTC(String expected, ZonedDateTime actual) {
		Assertions.assertEquals(LocalDateTime.parse(expected, DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneId.of("UTC")),
				actual.withZoneSameInstant(ZoneId.of("UTC")));
	}

}
