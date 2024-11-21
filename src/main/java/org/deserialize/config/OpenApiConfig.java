package com.enel.eic.commons.config;

import com.enel.eic.commons.util.ApplicationContextUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${application.name}")
    private String projectName;

    @Value("${application.version}")
    private String projectVersion;

    @Value("${application.description}")
    private String projectDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.info(new Info().title(projectName).version(StringUtils.defaultIfBlank(ApplicationContextUtils.getReleaseVersion(), projectVersion)).description(projectDescription));
        return openAPI;
    }
}

