package com.andon.tradeprocessing.service;

import com.andon.tradeprocessing.dto.PortfolioResponse;
import com.andon.tradeprocessing.dto.TradeResponse;
import com.andon.tradeprocessing.entity.Account;
import com.andon.tradeprocessing.entity.MarketPrice;
import com.andon.tradeprocessing.entity.Position;
import com.andon.tradeprocessing.entity.Trade;
import com.andon.tradeprocessing.repository.PositionRepository;
import com.andon.tradeprocessing.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AccountService     accountService;
    private final PositionRepository positionRepository;
    private final TradeRepository    tradeRepository;
    private final MarketPriceService marketPriceService;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long accountId) {
        Account account = accountService.findById(accountId);
        List<Position> positions = positionRepository.findByAccountId(accountId);

        List<PortfolioResponse.PositionResponse> positionResponses = positions.stream()
                .map(this::toPositionResponse)
                .toList();

        BigDecimal totalMarketValue = positionResponses.stream()
                .map(PortfolioResponse.PositionResponse::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUnrealizedPnL = positionResponses.stream()
                .map(PortfolioResponse.PositionResponse::getUnrealizedPnL)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortfolioResponse.builder()
                .accountId(accountId)
                .username(account.getUsername())
                .cashBalance(account.getCashBalance())
                .positionsMarketValue(totalMarketValue)
                .totalPortfolioValue(account.getCashBalance().add(totalMarketValue))
                .unrealizedPnL(totalUnrealizedPnL)
                .positions(positionResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TradeResponse> getTradeHistory(Long accountId) {
        accountService.findById(accountId);
        return tradeRepository.findByAccountId(accountId)
                .stream().map(this::toTradeResponse).toList();
    }

    private PortfolioResponse.PositionResponse toPositionResponse(Position pos) {
        MarketPrice mp = marketPriceService.getMarketPrice(pos.getSymbol());
        BigDecimal currentPrice = mp.getCurrentPrice();
        BigDecimal marketValue  = currentPrice.multiply(BigDecimal.valueOf(pos.getQuantity()));
        BigDecimal costBasis    = pos.getAverageCost().multiply(BigDecimal.valueOf(pos.getQuantity()));
        BigDecimal unrealizedPnL = marketValue.subtract(costBasis);
        BigDecimal pnlPercent = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : unrealizedPnL.divide(costBasis, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

        return PortfolioResponse.PositionResponse.builder()
                .symbol(pos.getSymbol())
                .companyName(mp.getCompanyName())
                .quantity(pos.getQuantity())
                .averageCost(pos.getAverageCost())
                .currentPrice(currentPrice)
                .marketValue(marketValue)
                .unrealizedPnL(unrealizedPnL)
                .unrealizedPnLPercent(pnlPercent.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private TradeResponse toTradeResponse(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .orderId(trade.getOrder().getId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .executionPrice(trade.getExecutionPrice())
                .totalValue(trade.getTotalValue())
                .executedAt(trade.getExecutedAt())
                .build();
    }
}
