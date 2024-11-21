package com.enel.eic.commons.security.validation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XContentTypeOptionsHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests((authorize) -> authorize
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/**").permitAll()
            )
            .headers(headers -> headers
                .addHeaderWriter(new XContentTypeOptionsHeaderWriter())
                .httpStrictTransportSecurity( sts -> sts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(31536000)
                )
            );
        return http.build();
    }
}
