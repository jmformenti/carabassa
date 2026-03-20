package org.atypical.carabassa.restapi.rdbms.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.atypical.carabassa.restapi.rdbms.mapper"})
public class RestApiRdbmsMapperConfiguration {

}
