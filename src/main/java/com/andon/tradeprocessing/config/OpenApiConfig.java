package com.andon.tradeprocessing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trade Processing API")
                        .description("""
                                Simplified stock brokerage backend demonstrating:
                                - Trade lifecycle: order submission → matching → settlement
                                - MARKET and LIMIT order types (BUY/SELL)
                                - Portfolio management with real-time P&L
                                - Event-driven architecture via Apache Kafka (TradeExecuted events)
                                - Order book with pending limit order visibility
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Andon LaFreniere")
                                .url("https://github.com/Andon-LaFreniere/Trade_Processing"))
                        .license(new License().name("MIT")));
    }
}
