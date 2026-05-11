package org.atypical.carabassa.engine.llm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
@ComponentScan(basePackages = "org.atypical.carabassa.engine.llm")
public class LlmConfiguration {

}
