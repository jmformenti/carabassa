package org.atypical.carabassa.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.atypical.carabassa.core.configuration.CoreConfiguration;
import org.atypical.carabassa.indexer.rdbms.configuration.IndexerRdbmsConfiguration;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.rdbms.configuration.RestApiRdbmsMapperConfiguration;
import org.atypical.carabassa.storage.fs.configuration.StorageFSConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.util.FileSystemUtils;

@Import({ CoreConfiguration.class, RestApiConfiguration.class, RestApiRdbmsMapperConfiguration.class,
		IndexerRdbmsConfiguration.class, StorageFSConfiguration.class })
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	@Value("${carabassa.tempdir}")
	private String tempDirLocation;

	@PostConstruct
	private void postConstruct() throws IOException {
		resetTempDir();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	private void resetTempDir() throws IOException {
		Path tempDirPath = Paths.get(tempDirLocation);
		FileSystemUtils.deleteRecursively(tempDirPath);
		Files.createDirectories(tempDirPath);
	}

}
