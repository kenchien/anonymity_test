package com.anonymity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI anonymityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("數據匿名化服務 API")
                        .description("提供完整的數據隱私保護解決方案，包括：\n" +
                                   "- k-Anonymity 匿名化\n" +
                                   "- l-Diversity 匿名化\n" +
                                   "- 差分隱私保護\n" +
                                   "- 數據效用評估\n" +
                                   "- 隱私風險分析")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("開發團隊")
                                .email("support@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8089")
                                .description("本地開發環境"),
                        new Server()
                                .url("https://api.example.com")
                                .description("生產環境")
                ))
                .tags(List.of(
                        new Tag().name("匿名化").description("k-Anonymity 和 l-Diversity 匿名化相關 API"),
                        new Tag().name("差分隱私").description("差分隱私保護相關 API"),
                        new Tag().name("數據評估").description("數據效用和隱私風險評估相關 API")
                ));
    }
} 