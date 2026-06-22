package com.andon.tradeprocessing.config;

import com.andon.tradeprocessing.dto.CreateAccountRequest;
import com.andon.tradeprocessing.service.AccountService;
import com.andon.tradeprocessing.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MarketPriceService marketPriceService;
    private final AccountService accountService;

    @Override
    public void run(String... args) {
        log.info("Seeding market prices...");
        marketPriceService.seedDefaultPrices();

        log.info("Creating demo account...");
        try {
            CreateAccountRequest demo = new CreateAccountRequest();
            demo.setUsername("demo_trader");
            demo.setEmail("demo@tradeprocessing.com");
            demo.setInitialDeposit(new BigDecimal("100000.00"));
            accountService.createAccount(demo);
            log.info("Demo account created: username=demo_trader balance=$100,000.00 (accountId=1)");
        } catch (IllegalStateException e) {
            log.info("Demo account already exists, skipping");
        }

        log.info("=== Trade Processing API ready ===");
        log.info("Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("H2 Console: http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:tradedb)");
    }
}
