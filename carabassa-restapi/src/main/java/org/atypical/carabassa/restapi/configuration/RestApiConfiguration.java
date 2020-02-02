package org.atypical.carabassa.restapi.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.atypical.carabassa.restapi.controller", "org.atypical.carabassa.restapi.mapper",
		"org.atypical.carabassa.restapi.representation.assembler" })
public class RestApiConfiguration {

}
