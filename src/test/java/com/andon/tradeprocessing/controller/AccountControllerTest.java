package com.andon.tradeprocessing.controller;

import com.andon.tradeprocessing.dto.CreateAccountRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@DisplayName("Account API Integration Tests")
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/accounts - creates account with valid request")
    void createAccount_success() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setUsername("trader_jane");
        req.setEmail("jane@example.com");
        req.setInitialDeposit(new BigDecimal("25000.00"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("trader_jane"))
                .andExpect(jsonPath("$.data.cashBalance").value(25000.00));
    }

    @Test
    @DisplayName("POST /api/accounts - rejects invalid email")
    void createAccount_invalidEmail_returns400() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setUsername("trader_x");
        req.setEmail("not-an-email");
        req.setInitialDeposit(new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/accounts - rejects deposit below minimum")
    void createAccount_belowMinDeposit_returns400() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setUsername("trader_poor");
        req.setEmail("poor@example.com");
        req.setInitialDeposit(new BigDecimal("50.00")); // below $100 minimum

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/accounts/{id} - returns 404 for unknown account")
    void getAccount_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/accounts/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
