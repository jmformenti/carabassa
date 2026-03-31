package org.atypical.carabassa.cli.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@Import(value = { org.atypical.carabassa.core.component.util.LocalizedMessage.class })
public class CliConfiguration {

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder(
            @Value("${carabassa.auth.token:}") String token) {
        WebClient.Builder builder = WebClient.builder();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return builder;
    }

}