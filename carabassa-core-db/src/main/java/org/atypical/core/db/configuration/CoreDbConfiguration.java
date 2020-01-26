package org.atypical.core.db.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = { "org.atypical.core.db.component" })
@EnableJpaRepositories(basePackages = { "org.atypical.core.db.repository" })
@EntityScan({ "org.atypical.core.db.entity" })
public class CoreDbConfiguration {

}
