package com.andon.tradeprocessing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PortfolioResponse {

    private Long accountId;
    private String username;

    /** Cash available to trade */
    private BigDecimal cashBalance;

    /** Sum of current market value of all positions */
    private BigDecimal positionsMarketValue;

    /** cashBalance + positionsMarketValue */
    private BigDecimal totalPortfolioValue;

    /** Total unrealized P&L across all positions */
    private BigDecimal unrealizedPnL;

    private List<PositionResponse> positions;

    @Data
    @Builder
    public static class PositionResponse {
        private String symbol;
        private String companyName;
        private Integer quantity;
        private BigDecimal averageCost;
        private BigDecimal currentPrice;
        private BigDecimal marketValue;
        private BigDecimal unrealizedPnL;
        private BigDecimal unrealizedPnLPercent;
    }
}
