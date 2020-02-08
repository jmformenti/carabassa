package org.atypical.carabassa.core.db.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = { "org.atypical.carabassa.core.db.component" })
@EnableJpaRepositories(basePackages = { "org.atypical.carabassa.core.db.repository" })
@EntityScan({ "org.atypical.carabassa.core.db.entity" })
public class CoreDbConfiguration {

}
