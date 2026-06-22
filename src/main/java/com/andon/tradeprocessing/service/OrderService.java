package com.andon.tradeprocessing.service;

import com.andon.tradeprocessing.dto.OrderResponse;
import com.andon.tradeprocessing.dto.SubmitOrderRequest;
import com.andon.tradeprocessing.entity.Account;
import com.andon.tradeprocessing.entity.Order;
import com.andon.tradeprocessing.enums.OrderStatus;
import com.andon.tradeprocessing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository      orderRepository;
    private final AccountService       accountService;
    private final OrderMatchingEngine  matchingEngine;

    @Transactional
    public OrderResponse submitOrder(SubmitOrderRequest request) {
        Account account = accountService.findById(request.getAccountId());
        Order order = matchingEngine.submitOrder(
                account,
                request.getSymbol(),
                request.getSide(),
                request.getType(),
                request.getQuantity(),
                request.getLimitPrice()
        );
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long accountId) {
        return toResponse(matchingEngine.cancelOrder(orderId, accountId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForAccount(Long accountId) {
        accountService.findById(accountId); // validate account exists
        return orderRepository.findByAccountIdOrderBySubmittedAtDesc(accountId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingOrders(Long accountId) {
        accountService.findById(accountId);
        return orderRepository.findByAccountIdAndStatusOrderBySubmittedAtDesc(accountId, OrderStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .accountId(order.getAccount().getId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .status(order.getStatus())
                .quantity(order.getQuantity())
                .limitPrice(order.getLimitPrice())
                .fillPrice(order.getFillPrice())
                .submittedAt(order.getSubmittedAt())
                .filledAt(order.getFilledAt())
                .build();
    }
}
