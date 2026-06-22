package com.andon.tradeprocessing.repository;

import com.andon.tradeprocessing.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    @Query("SELECT t FROM Trade t WHERE t.order.account.id = :accountId ORDER BY t.executedAt DESC")
    List<Trade> findByAccountId(@Param("accountId") Long accountId);

    List<Trade> findBySymbolOrderByExecutedAtDesc(String symbol);
}
