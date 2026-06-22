package com.andon.tradeprocessing.entity;

import com.andon.tradeprocessing.enums.OrderSide;
import com.andon.tradeprocessing.enums.OrderStatus;
import com.andon.tradeprocessing.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * For LIMIT orders: the maximum (BUY) or minimum (SELL) acceptable price.
     * For MARKET orders: null at submission, set to fill price after execution.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal limitPrice;

    /**
     * The actual price at which the order was filled. Null until filled.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal fillPrice;

    @Column(updatable = false, nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime filledAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
