package org.atypical.carabassa.cli.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Import(value = { org.atypical.carabassa.core.component.util.LocalizedMessage.class })
public class CliConfiguration {

	@Autowired
	private ObjectMapper objectMapper;

	@PostConstruct
	private void postConstruct() {
		this.objectMapper.registerModule(new Jackson2HalModule());
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

}