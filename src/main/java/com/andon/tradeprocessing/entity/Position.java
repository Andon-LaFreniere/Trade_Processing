package com.andon.tradeprocessing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A Position tracks an account's current holding of a given symbol:
 * how many shares and at what average cost basis.
 */
@Entity
@Table(name = "positions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "symbol"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Average cost per share across all BUY fills.
     * Updated on each new buy using weighted average formula.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
