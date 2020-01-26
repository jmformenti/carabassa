package org.atypical.carabassa.core.db.test.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class TestConfiguration {

	@PostConstruct
	private void postConstruct() throws IOException {
		// a progamatically way to create repo dir before DatasetFSStorage's
		// postconstruct
		InputStream stream = new ClassPathResource("application.properties").getInputStream();
		Properties prop = new Properties();
		prop.load(stream);
		Files.createDirectories(Paths.get(prop.getProperty("carabassa.repodir")));
	}

	@Bean
	public MessageSource messageSource() {
		// default message source to avoid NoSuchMessageException during tests
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setUseCodeAsDefaultMessage(true);
		return messageSource;
	}
}
