package com.andon.tradeprocessing.dto;

import com.andon.tradeprocessing.enums.OrderSide;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TradeResponse {
    private Long id;
    private Long orderId;
    private String symbol;
    private OrderSide side;
    private Integer quantity;
    private BigDecimal executionPrice;
    private BigDecimal totalValue;
    private LocalDateTime executedAt;
}
