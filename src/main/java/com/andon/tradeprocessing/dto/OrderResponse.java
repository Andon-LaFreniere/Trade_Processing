package com.andon.tradeprocessing.dto;

import com.andon.tradeprocessing.enums.OrderSide;
import com.andon.tradeprocessing.enums.OrderStatus;
import com.andon.tradeprocessing.enums.OrderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private Long accountId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private OrderStatus status;
    private Integer quantity;
    private BigDecimal limitPrice;
    private BigDecimal fillPrice;
    private LocalDateTime submittedAt;
    private LocalDateTime filledAt;
}
