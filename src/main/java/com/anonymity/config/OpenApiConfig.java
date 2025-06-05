package com.anonymity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI anonymityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("數據匿名化服務 API")
                        .description("提供k-Anonymity和l-Diversity匿名化功能的REST API服務")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("開發團隊")
                                .email("support@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
} 