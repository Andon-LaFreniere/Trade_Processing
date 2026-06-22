package com.andon.tradeprocessing.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "100.00", message = "Minimum initial deposit is $100.00")
    @DecimalMax(value = "10000000.00", message = "Maximum initial deposit is $10,000,000.00")
    private BigDecimal initialDeposit;
}
