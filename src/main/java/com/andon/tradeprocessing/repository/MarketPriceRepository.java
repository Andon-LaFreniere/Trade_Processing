package com.andon.tradeprocessing.repository;

import com.andon.tradeprocessing.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, String> {
    boolean existsBySymbol(String symbol);
}
