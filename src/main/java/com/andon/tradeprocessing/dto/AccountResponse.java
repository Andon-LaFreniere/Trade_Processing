package com.andon.tradeprocessing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private Long id;
    private String username;
    private String email;
    private BigDecimal cashBalance;
    private LocalDateTime createdAt;
}
