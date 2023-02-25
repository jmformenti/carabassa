package org.atypical.carabassa.core.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.atypical.carabassa.core.service", "org.atypical.carabassa.core.component" })
public class CoreConfiguration {

}
