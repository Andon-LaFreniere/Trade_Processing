package com.andon.tradeprocessing.controller;

import com.andon.tradeprocessing.dto.ApiResponse;
import com.andon.tradeprocessing.dto.OrderResponse;
import com.andon.tradeprocessing.dto.SubmitOrderRequest;
import com.andon.tradeprocessing.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Trade order submission and management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Submit a new trade order (BUY or SELL, MARKET or LIMIT)")
    public ResponseEntity<ApiResponse<OrderResponse>> submitOrder(
            @Valid @RequestBody SubmitOrderRequest request) {
        OrderResponse response = orderService.submitOrder(request);
        String message = response.getStatus().name().equals("FILLED")
                ? "Order filled immediately at $" + response.getFillPrice()
                : "Order queued (LIMIT price not yet met)";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(message, response));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel a pending order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", orderService.cancelOrder(orderId, accountId)));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all orders for an account")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersForAccount(accountId)));
    }

    @GetMapping("/account/{accountId}/pending")
    @Operation(summary = "Get pending (unexecuted) limit orders for an account")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingOrders(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getPendingOrders(accountId)));
    }
}
