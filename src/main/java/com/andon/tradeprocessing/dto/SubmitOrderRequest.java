package com.andon.tradeprocessing.dto;

import com.andon.tradeprocessing.enums.OrderSide;
import com.andon.tradeprocessing.enums.OrderType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitOrderRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotBlank(message = "Symbol is required")
    @Size(min = 1, max = 10, message = "Symbol must be 1-10 characters")
    private String symbol;

    @NotNull(message = "Order side (BUY/SELL) is required")
    private OrderSide side;

    @NotNull(message = "Order type (MARKET/LIMIT) is required")
    private OrderType type;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100000, message = "Quantity cannot exceed 100,000 shares per order")
    private Integer quantity;

    /**
     * Required if type == LIMIT. Ignored for MARKET orders.
     */
    @DecimalMin(value = "0.01", message = "Limit price must be greater than $0.01")
    private BigDecimal limitPrice;
}
