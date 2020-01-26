package org.atypical.core.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.atypical.core.service", "org.atypical.core.component" })
public class CoreConfiguration {

}
