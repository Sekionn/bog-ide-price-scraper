package dk.sebastian.pricescraper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI priceScraperOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bog & ide price scraper API")
                        .description("API for reading stored Bog & ide product prices and managing scraper refresh runs.")
                        .version("0.0.1"));
    }
}
