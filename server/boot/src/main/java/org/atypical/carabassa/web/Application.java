package org.atypical.carabassa.web;

import org.atypical.carabassa.core.configuration.CoreConfiguration;
import org.atypical.carabassa.indexer.rdbms.configuration.IndexerRdbmsConfiguration;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.rdbms.configuration.RestApiRdbmsMapperConfiguration;
import org.atypical.carabassa.storage.fs.configuration.StorageFSConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.util.FileSystemUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication(scanBasePackageClasses = {CoreConfiguration.class, RestApiConfiguration.class, RestApiRdbmsMapperConfiguration.class,
        IndexerRdbmsConfiguration.class, StorageFSConfiguration.class})
public class Application extends SpringBootServletInitializer {

    @Value("${carabassa.tempdir:#{null}}")
    private String tempDirLocation;

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void postConstruct() throws IOException {
        resetTempDir();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private void resetTempDir() throws IOException {
        if (tempDirLocation != null) {
            Path tempDirPath = Paths.get(tempDirLocation);
            FileSystemUtils.deleteRecursively(tempDirPath);
            Files.createDirectories(tempDirPath);
        }
    }

}
