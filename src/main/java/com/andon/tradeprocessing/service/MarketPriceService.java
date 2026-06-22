package com.andon.tradeprocessing.service;

import com.andon.tradeprocessing.entity.MarketPrice;
import com.andon.tradeprocessing.exception.ResourceNotFoundException;
import com.andon.tradeprocessing.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;

    @Transactional(readOnly = true)
    public BigDecimal getPrice(String symbol) {
        return marketPriceRepository.findById(symbol.toUpperCase())
                .map(MarketPrice::getCurrentPrice)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No market price found for symbol '" + symbol + "'. Use /api/market/price to set one."));
    }

    @Transactional(readOnly = true)
    public MarketPrice getMarketPrice(String symbol) {
        return marketPriceRepository.findById(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Symbol not found: " + symbol));
    }

    @Transactional(readOnly = true)
    public List<MarketPrice> getAllPrices() {
        return marketPriceRepository.findAll();
    }

    @Transactional
    public MarketPrice setPrice(String symbol, String companyName, BigDecimal price) {
        MarketPrice mp = marketPriceRepository.findById(symbol.toUpperCase())
                .orElse(MarketPrice.builder().symbol(symbol.toUpperCase()).build());
        mp.setCompanyName(companyName);
        mp.setCurrentPrice(price);
        MarketPrice saved = marketPriceRepository.save(mp);
        log.info("Market price updated: symbol={} price={}", symbol, price);
        return saved;
    }

    @Transactional
    public void seedDefaultPrices() {
        Map<String, Object[]> defaults = Map.of(
            "AAPL",  new Object[]{"Apple Inc.",            new BigDecimal("189.50")},
            "MSFT",  new Object[]{"Microsoft Corporation", new BigDecimal("415.20")},
            "GOOGL", new Object[]{"Alphabet Inc.",         new BigDecimal("173.80")},
            "AMZN",  new Object[]{"Amazon.com Inc.",       new BigDecimal("195.40")},
            "NVDA",  new Object[]{"NVIDIA Corporation",    new BigDecimal("875.00")},
            "JPM",   new Object[]{"JPMorgan Chase & Co.",  new BigDecimal("198.60")},
            "TSLA",  new Object[]{"Tesla Inc.",            new BigDecimal("245.30")},
            "META",  new Object[]{"Meta Platforms Inc.",   new BigDecimal("521.40")}
        );

        defaults.forEach((symbol, data) -> {
            if (!marketPriceRepository.existsBySymbol(symbol)) {
                setPrice(symbol, (String) data[0], (BigDecimal) data[1]);
            }
        });

        log.info("Seeded {} default market prices", defaults.size());
    }

    public boolean symbolExists(String symbol) {
        return marketPriceRepository.existsBySymbol(symbol.toUpperCase());
    }
}
