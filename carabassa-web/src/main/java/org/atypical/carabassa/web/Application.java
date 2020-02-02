package org.atypical.carabassa.web;

import org.atypical.carabassa.core.configuration.CoreConfiguration;
import org.atypical.carabassa.core.db.configuration.CoreDbConfiguration;
import org.atypical.carabassa.restapi.configuration.RestApiConfiguration;
import org.atypical.carabassa.restapi.db.configuration.RestApiMapperConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@Import({ CoreConfiguration.class, CoreDbConfiguration.class, RestApiConfiguration.class, RestApiMapperConfiguration.class })
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
