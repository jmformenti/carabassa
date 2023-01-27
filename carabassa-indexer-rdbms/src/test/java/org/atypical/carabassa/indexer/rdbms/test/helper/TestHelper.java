package org.atypical.carabassa.indexer.rdbms.test.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Assertions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class TestHelper {

	public static Resource getImageResource(String filename) throws IOException {
		return getTempResource(new ClassPathResource("images/" + filename));
	}

	private static Resource getTempResource(Resource resource) throws IOException {
		File tempFile = File.createTempFile("test", null);
		tempFile.deleteOnExit();
		Files.copy(resource.getInputStream(), Paths.get(tempFile.getPath()), StandardCopyOption.REPLACE_EXISTING);
		return new FileSystemResource(tempFile);
	}

	public static void assertDateInUTC(String expected, ZonedDateTime actual) {
		Assertions.assertEquals(LocalDateTime.parse(expected, DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneId.of("UTC")),
				actual.withZoneSameInstant(ZoneId.of("UTC")));
	}

}
