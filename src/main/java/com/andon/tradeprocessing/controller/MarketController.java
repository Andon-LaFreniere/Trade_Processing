package com.andon.tradeprocessing.controller;

import com.andon.tradeprocessing.dto.ApiResponse;
import com.andon.tradeprocessing.entity.MarketPrice;
import com.andon.tradeprocessing.enums.OrderStatus;
import com.andon.tradeprocessing.repository.OrderRepository;
import com.andon.tradeprocessing.service.MarketPriceService;
import com.andon.tradeprocessing.service.OrderMatchingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Market", description = "Market prices and order book")
public class MarketController {

    private final MarketPriceService  marketPriceService;
    private final OrderMatchingEngine matchingEngine;
    private final OrderRepository     orderRepository;

    @GetMapping("/prices")
    @Operation(summary = "List all available symbols and their current prices")
    public ResponseEntity<ApiResponse<List<MarketPrice>>> getAllPrices() {
        return ResponseEntity.ok(ApiResponse.ok(marketPriceService.getAllPrices()));
    }

    @GetMapping("/prices/{symbol}")
    @Operation(summary = "Get current price for a symbol")
    public ResponseEntity<ApiResponse<MarketPrice>> getPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(ApiResponse.ok(marketPriceService.getMarketPrice(symbol.toUpperCase())));
    }

    @PutMapping("/prices/{symbol}")
    @Operation(summary = "Update market price for a symbol (triggers limit order sweep)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePrice(
            @PathVariable @NotBlank String symbol,
            @RequestParam @NotBlank String companyName,
            @RequestParam @DecimalMin("0.01") BigDecimal price) {

        MarketPrice updated = marketPriceService.setPrice(symbol.toUpperCase(), companyName, price);
        int filled = matchingEngine.sweepPendingLimitOrders(symbol.toUpperCase());

        return ResponseEntity.ok(ApiResponse.ok("Price updated", Map.of(
                "symbol", updated.getSymbol(),
                "companyName", updated.getCompanyName(),
                "newPrice", updated.getCurrentPrice(),
                "limitOrdersFilled", filled
        )));
    }

    @GetMapping("/orderbook/{symbol}")
    @Operation(summary = "View pending limit orders for a symbol (the order book)")
    public ResponseEntity<ApiResponse<Object>> getOrderBook(@PathVariable String symbol) {
        var pending = orderRepository.findBySymbolAndStatus(symbol.toUpperCase(), OrderStatus.PENDING);
        var buys  = pending.stream().filter(o -> o.getSide().name().equals("BUY"))
                .map(o -> Map.of("orderId", o.getId(), "qty", o.getQuantity(), "limit", o.getLimitPrice()))
                .toList();
        var sells = pending.stream().filter(o -> o.getSide().name().equals("SELL"))
                .map(o -> Map.of("orderId", o.getId(), "qty", o.getQuantity(), "limit", o.getLimitPrice()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "symbol", symbol.toUpperCase(),
                "bids", buys,
                "asks", sells
        )));
    }
}
