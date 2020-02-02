package org.atypical.carabassa.restapi.db.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.atypical.carabassa.restapi.db.mapper" })
public class RestApiMapperConfiguration {

}
