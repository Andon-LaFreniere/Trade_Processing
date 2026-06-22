package com.andon.tradeprocessing.controller;

import com.andon.tradeprocessing.dto.ApiResponse;
import com.andon.tradeprocessing.dto.PortfolioResponse;
import com.andon.tradeprocessing.dto.TradeResponse;
import com.andon.tradeprocessing.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio valuation and trade history")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/{accountId}")
    @Operation(summary = "Get full portfolio snapshot: positions, market value, unrealized P&L")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getPortfolio(accountId)));
    }

    @GetMapping("/{accountId}/trades")
    @Operation(summary = "Get trade history for an account")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> getTradeHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getTradeHistory(accountId)));
    }
}
