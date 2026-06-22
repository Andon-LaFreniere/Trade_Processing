package com.andon.tradeprocessing.dto;

import com.andon.tradeprocessing.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published to the "trade-executed" Kafka topic whenever an order is filled.
 * Consumers (audit, risk, reporting) subscribe to this topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutedEvent {
    private Long tradeId;
    private Long orderId;
    private Long accountId;
    private String symbol;
    private OrderSide side;
    private Integer quantity;
    private BigDecimal executionPrice;
    private BigDecimal totalValue;
    private LocalDateTime executedAt;
}
