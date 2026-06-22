package com.andon.tradeprocessing.repository;

import com.andon.tradeprocessing.entity.Order;
import com.andon.tradeprocessing.enums.OrderSide;
import com.andon.tradeprocessing.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByAccountIdOrderBySubmittedAtDesc(Long accountId);

    List<Order> findByAccountIdAndStatusOrderBySubmittedAtDesc(Long accountId, OrderStatus status);

    /**
     * Find all pending LIMIT orders for a symbol that can be matched:
     * - BUY orders with limitPrice >= marketPrice
     * - SELL orders with limitPrice <= marketPrice
     */
    @Query("""
            SELECT o FROM Order o
            WHERE o.symbol = :symbol
              AND o.status = 'PENDING'
              AND o.type = 'LIMIT'
              AND o.side = :side
              AND ((:side = 'BUY' AND o.limitPrice >= :marketPrice)
                OR (:side = 'SELL' AND o.limitPrice <= :marketPrice))
            ORDER BY o.submittedAt ASC
            """)
    List<Order> findMatchableLimitOrders(
            @Param("symbol") String symbol,
            @Param("side") OrderSide side,
            @Param("marketPrice") BigDecimal marketPrice
    );

    List<Order> findBySymbolAndStatus(String symbol, OrderStatus status);
}
