package org.atypical.restapi.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.atypical.restapi.controller", "org.atypical.restapi.mapper" })
public class RestApiConfiguration {

}
