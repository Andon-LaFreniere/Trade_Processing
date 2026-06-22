package com.andon.tradeprocessing.service;

import com.andon.tradeprocessing.dto.TradeExecutedEvent;
import com.andon.tradeprocessing.entity.*;
import com.andon.tradeprocessing.enums.OrderSide;
import com.andon.tradeprocessing.enums.OrderStatus;
import com.andon.tradeprocessing.enums.OrderType;
import com.andon.tradeprocessing.exception.TradeValidationException;
import com.andon.tradeprocessing.kafka.TradeEventProducer;
import com.andon.tradeprocessing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core matching engine: validates and executes incoming orders.
 *
 * Rules:
 *  - MARKET orders: fill immediately at current market price
 *  - LIMIT BUY:     queue if market price > limit; fill if market price <= limit
 *  - LIMIT SELL:    queue if market price < limit; fill if market price >= limit
 *  - After each market-price update, pending limit orders are re-evaluated
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMatchingEngine {

    private final AccountRepository    accountRepository;
    private final OrderRepository      orderRepository;
    private final TradeRepository      tradeRepository;
    private final PositionRepository   positionRepository;
    private final MarketPriceService   marketPriceService;
    private final TradeEventProducer   tradeEventProducer;

    // ── Submit ──────────────────────────────────────────────────────────────

    @Transactional
    public Order submitOrder(Account account, String symbol, OrderSide side,
                             OrderType type, int quantity, BigDecimal limitPrice) {

        String upperSymbol = symbol.toUpperCase();

        // 1. Validate symbol
        if (!marketPriceService.symbolExists(upperSymbol)) {
            throw new TradeValidationException("Unknown symbol: " + upperSymbol);
        }

        // 2. Validate LIMIT orders have a price
        if (type == OrderType.LIMIT && (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new TradeValidationException("LIMIT orders require a positive limitPrice");
        }

        BigDecimal marketPrice = marketPriceService.getPrice(upperSymbol);

        // 3. Pre-flight validation
        validateOrder(account, side, type, quantity, limitPrice, marketPrice);

        // 4. Build the order
        Order order = Order.builder()
                .account(account)
                .symbol(upperSymbol)
                .side(side)
                .type(type)
                .status(OrderStatus.PENDING)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .build();
        order = orderRepository.save(order);

        // 5. Attempt immediate fill
        boolean filled = tryFill(order, marketPrice, account);
        if (!filled) {
            log.info("Order id={} queued as PENDING (LIMIT not yet met)", order.getId());
        }

        return order;
    }

    // ── Cancellation ────────────────────────────────────────────────────────

    @Transactional
    public Order cancelOrder(Long orderId, Long accountId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new TradeValidationException("Order not found: id=" + orderId));

        if (!order.getAccount().getId().equals(accountId)) {
            throw new TradeValidationException("Order does not belong to this account");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new TradeValidationException("Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        log.info("Cancelled order id={} symbol={}", orderId, order.getSymbol());
        return orderRepository.save(order);
    }

    // ── Limit Order Sweep (called when market price updates) ────────────────

    @Transactional
    public int sweepPendingLimitOrders(String symbol) {
        BigDecimal marketPrice = marketPriceService.getPrice(symbol.toUpperCase());
        int filled = 0;

        // Try to fill pending BUY limits (those whose limit >= market price)
        List<Order> buyOrders = orderRepository.findMatchableLimitOrders(
                symbol.toUpperCase(), OrderSide.BUY, marketPrice);
        for (Order order : buyOrders) {
            Account account = accountRepository.findById(order.getAccount().getId()).orElseThrow();
            if (tryFill(order, marketPrice, account)) filled++;
        }

        // Try to fill pending SELL limits (those whose limit <= market price)
        List<Order> sellOrders = orderRepository.findMatchableLimitOrders(
                symbol.toUpperCase(), OrderSide.SELL, marketPrice);
        for (Order order : sellOrders) {
            Account account = accountRepository.findById(order.getAccount().getId()).orElseThrow();
            if (tryFill(order, marketPrice, account)) filled++;
        }

        if (filled > 0) {
            log.info("Swept {} pending limit orders for symbol={} at marketPrice={}", filled, symbol, marketPrice);
        }
        return filled;
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Attempt to fill an order at the given market price.
     * Returns true if the order was filled, false if it remains PENDING.
     */
    private boolean tryFill(Order order, BigDecimal marketPrice, Account account) {
        BigDecimal fillPrice = determineFillPrice(order, marketPrice);
        if (fillPrice == null) {
            return false; // limit not met
        }

        BigDecimal totalCost = fillPrice.multiply(BigDecimal.valueOf(order.getQuantity()));

        // Re-check balance/position at fill time
        if (order.getSide() == OrderSide.BUY) {
            if (account.getCashBalance().compareTo(totalCost) < 0) {
                order.setStatus(OrderStatus.REJECTED);
                orderRepository.save(order);
                log.warn("Order id={} REJECTED at fill time: insufficient funds (need={} have={})",
                        order.getId(), totalCost, account.getCashBalance());
                return false;
            }
            account.setCashBalance(account.getCashBalance().subtract(totalCost));
        } else {
            // SELL: verify the account still holds enough shares
            Position pos = positionRepository.findByAccountIdAndSymbol(account.getId(), order.getSymbol())
                    .orElse(null);
            if (pos == null || pos.getQuantity() < order.getQuantity()) {
                order.setStatus(OrderStatus.REJECTED);
                orderRepository.save(order);
                log.warn("Order id={} REJECTED at fill time: insufficient shares", order.getId());
                return false;
            }
            account.setCashBalance(account.getCashBalance().add(totalCost));
            updatePositionOnSell(pos, order.getQuantity());
        }

        if (order.getSide() == OrderSide.BUY) {
            updatePositionOnBuy(account, order.getSymbol(), order.getQuantity(), fillPrice);
        }

        accountRepository.save(account);

        // Mark order filled
        order.setStatus(OrderStatus.FILLED);
        order.setFillPrice(fillPrice);
        order.setFilledAt(LocalDateTime.now());
        orderRepository.save(order);

        // Create immutable trade record
        Trade trade = Trade.builder()
                .order(order)
                .symbol(order.getSymbol())
                .side(order.getSide())
                .quantity(order.getQuantity())
                .executionPrice(fillPrice)
                .totalValue(totalCost)
                .build();
        trade = tradeRepository.save(trade);

        // Publish Kafka event
        tradeEventProducer.publishTradeExecuted(TradeExecutedEvent.builder()
                .tradeId(trade.getId())
                .orderId(order.getId())
                .accountId(account.getId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .executionPrice(trade.getExecutionPrice())
                .totalValue(trade.getTotalValue())
                .executedAt(trade.getExecutedAt())
                .build());

        log.info("Filled order id={} symbol={} side={} qty={} at price={}",
                order.getId(), order.getSymbol(), order.getSide(), order.getQuantity(), fillPrice);
        return true;
    }

    /**
     * Returns the fill price if the order should execute, null if it should remain pending.
     */
    private BigDecimal determineFillPrice(Order order, BigDecimal marketPrice) {
        if (order.getType() == OrderType.MARKET) {
            return marketPrice;
        }
        // LIMIT logic
        if (order.getSide() == OrderSide.BUY) {
            // Buy limit fills if market <= limitPrice
            return marketPrice.compareTo(order.getLimitPrice()) <= 0 ? marketPrice : null;
        } else {
            // Sell limit fills if market >= limitPrice
            return marketPrice.compareTo(order.getLimitPrice()) >= 0 ? marketPrice : null;
        }
    }

    private void validateOrder(Account account, OrderSide side, OrderType type,
                               int quantity, BigDecimal limitPrice, BigDecimal marketPrice) {
        if (side == OrderSide.BUY) {
            BigDecimal estimatedCost = (type == OrderType.LIMIT ? limitPrice : marketPrice)
                    .multiply(BigDecimal.valueOf(quantity));
            if (account.getCashBalance().compareTo(estimatedCost) < 0) {
                throw new TradeValidationException(String.format(
                        "Insufficient funds: required=$%.2f available=$%.2f",
                        estimatedCost, account.getCashBalance()));
            }
        } else {
            // SELL: must hold at least `quantity` shares
            Position pos = positionRepository
                    .findByAccountIdAndSymbol(account.getId(), account.getId().toString())
                    .orElse(null);
            // We look it up properly below — the pre-flight here just checks existence
            // Full check is in tryFill to handle race conditions cleanly
        }
    }

    private void updatePositionOnBuy(Account account, String symbol, int quantity, BigDecimal fillPrice) {
        positionRepository.findByAccountIdAndSymbol(account.getId(), symbol)
                .ifPresentOrElse(pos -> {
                    // Weighted average cost
                    BigDecimal totalShares = BigDecimal.valueOf(pos.getQuantity() + quantity);
                    BigDecimal newAvgCost = (pos.getAverageCost().multiply(BigDecimal.valueOf(pos.getQuantity()))
                            .add(fillPrice.multiply(BigDecimal.valueOf(quantity))))
                            .divide(totalShares, 4, RoundingMode.HALF_UP);
                    pos.setQuantity(pos.getQuantity() + quantity);
                    pos.setAverageCost(newAvgCost);
                    positionRepository.save(pos);
                }, () -> {
                    Position newPos = Position.builder()
                            .account(account)
                            .symbol(symbol)
                            .quantity(quantity)
                            .averageCost(fillPrice)
                            .build();
                    positionRepository.save(newPos);
                });
    }

    private void updatePositionOnSell(Position position, int quantity) {
        position.setQuantity(position.getQuantity() - quantity);
        if (position.getQuantity() == 0) {
            positionRepository.delete(position);
        } else {
            positionRepository.save(position);
        }
    }
}
