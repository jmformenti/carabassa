package org.atypical.carabassa.indexer.rdbms.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = {"org.atypical.carabassa.indexer.rdbms.component"})
@EnableJpaRepositories(basePackages = {"org.atypical.carabassa.indexer.rdbms.repository"})
@EntityScan({"org.atypical.carabassa.indexer.rdbms.entity"})
public class IndexerRdbmsConfiguration {

}
