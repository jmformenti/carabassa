package org.atypical.carabassa.security.configuration;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Root configuration for the security module.
 */
@Configuration
@ComponentScan(basePackages = {
        "org.atypical.carabassa.security.configuration",
        "org.atypical.carabassa.security.controller",
        "org.atypical.carabassa.security.filter",
        "org.atypical.carabassa.security.service"
})
@EnableJpaRepositories(basePackages = {"org.atypical.carabassa.security.repository"})
@EntityScan(basePackages = {"org.atypical.carabassa.security.entity"})
public class SecurityModuleConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
