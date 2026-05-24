package org.atypical.carabassa.web;

import org.atypical.carabassa.core.configuration.CoreConfiguration;
import org.atypical.carabassa.indexer.rdbms.configuration.IndexerRdbmsConfiguration;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.rdbms.configuration.RestApiRdbmsMapperConfiguration;
import org.atypical.carabassa.security.configuration.SecurityModuleConfiguration;
import org.atypical.carabassa.storage.fs.configuration.StorageFSConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.TimeZone;

@SpringBootApplication(scanBasePackageClasses = {CoreConfiguration.class, RestApiConfiguration.class, RestApiRdbmsMapperConfiguration.class,
        IndexerRdbmsConfiguration.class, StorageFSConfiguration.class, SecurityModuleConfiguration.class})
public class Application extends SpringBootServletInitializer {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${carabassa.tempdir:#{null}}")
    private String tempDirLocation;

    @Value("${carabassa.llm.url:}")
    private String llmUrl;

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void postConstruct() throws IOException {
        logger.info("Using repository dir: {}", System.getenv("CARABASSA_REPO_DIR"));
        if (llmUrl != null && !llmUrl.isEmpty()) {
            logger.info("Using LLM URL: {}", llmUrl);
        } else {
            logger.info("LLM is disabled (no URL configured)");
        }
        resetTempDir();
    }

    public static void main(String[] args) {
        // Forced timezone to UTC to avoid issues reading timestamps from database
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
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
