package com.andon.tradeprocessing.service;

import com.andon.tradeprocessing.entity.*;
import com.andon.tradeprocessing.enums.OrderSide;
import com.andon.tradeprocessing.enums.OrderStatus;
import com.andon.tradeprocessing.enums.OrderType;
import com.andon.tradeprocessing.exception.TradeValidationException;
import com.andon.tradeprocessing.kafka.TradeEventProducer;
import com.andon.tradeprocessing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderMatchingEngine Tests")
class OrderMatchingEngineTest {

    @Mock
    AccountRepository accountRepository;
    @Mock
    OrderRepository orderRepository;
    @Mock
    TradeRepository tradeRepository;
    @Mock
    PositionRepository positionRepository;
    @Mock
    MarketPriceService marketPriceService;
    @Mock
    TradeEventProducer tradeEventProducer;

    @InjectMocks
    OrderMatchingEngine engine;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .username("test_trader")
                .email("test@example.com")
                .cashBalance(new BigDecimal("50000.00"))
                .build();

        when(marketPriceService.symbolExists("AAPL")).thenReturn(true);
        when(marketPriceService.getPrice("AAPL")).thenReturn(new BigDecimal("189.50"));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) {
                /* simulate ID assignment */}
            return o;
        });
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── MARKET ORDER tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("MARKET BUY fills immediately at current price and deducts cash")
    void marketBuy_fillsImmediately() {
        when(positionRepository.findByAccountIdAndSymbol(1L, "AAPL")).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = engine.submitOrder(account, "AAPL", OrderSide.BUY, OrderType.MARKET, 10, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getFillPrice()).isEqualByComparingTo("189.50");
        // Cash: 50000 - (189.50 * 10) = 48105
        assertThat(account.getCashBalance()).isEqualByComparingTo("48105.00");
        verify(tradeEventProducer).publishTradeExecuted(any());
    }

    @Test
    @DisplayName("MARKET SELL fills immediately and credits cash")
    void marketSell_fillsImmediately() {
        Position pos = Position.builder()
                .account(account).symbol("AAPL").quantity(20).averageCost(new BigDecimal("150.00")).build();
        when(positionRepository.findByAccountIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(pos));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = engine.submitOrder(account, "AAPL", OrderSide.SELL, OrderType.MARKET, 5, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        // Cash: 50000 + (189.50 * 5) = 50947.50
        assertThat(account.getCashBalance()).isEqualByComparingTo("50947.50");
        assertThat(pos.getQuantity()).isEqualTo(15); // 20 - 5
        verify(tradeEventProducer).publishTradeExecuted(any());
    }

    @Test
    @DisplayName("MARKET BUY rejected when insufficient funds")
    void marketBuy_insufficientFunds_throws() {
        account.setCashBalance(new BigDecimal("100.00")); // only $100

        assertThatThrownBy(() -> engine.submitOrder(account, "AAPL", OrderSide.BUY, OrderType.MARKET, 100, null))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("Insufficient funds");
    }

    // ── LIMIT ORDER tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("LIMIT BUY queues as PENDING when market price exceeds limit")
    void limitBuy_queuesWhenMarketAboveLimit() {
        // Market = 189.50, limit = 180.00 → should queue
        Order order = engine.submitOrder(account, "AAPL", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("180.00"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getFillPrice()).isNull();
        verifyNoInteractions(tradeEventProducer);
    }

    @Test
    @DisplayName("LIMIT BUY fills when limit price >= market price")
    void limitBuy_fillsWhenLimitAboveMarket() {
        // Market = 189.50, limit = 195.00 → should fill immediately
        when(positionRepository.findByAccountIdAndSymbol(1L, "AAPL")).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = engine.submitOrder(account, "AAPL", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("195.00"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getFillPrice()).isEqualByComparingTo("189.50"); // filled at market, not limit
        verify(tradeEventProducer).publishTradeExecuted(any());
    }

    @Test
    @DisplayName("LIMIT order without price throws validation error")
    void limitOrder_withoutPrice_throws() {
        assertThatThrownBy(() -> engine.submitOrder(account, "AAPL", OrderSide.BUY, OrderType.LIMIT, 10, null))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("LIMIT orders require a positive limitPrice");
    }

    // ── Symbol validation ────────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown symbol is rejected")
    void unknownSymbol_throws() {
        when(marketPriceService.symbolExists("FAKE")).thenReturn(false);

        assertThatThrownBy(() -> engine.submitOrder(account, "FAKE", OrderSide.BUY, OrderType.MARKET, 1, null))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("Unknown symbol");
    }

    // ── Position average cost ─────────────────────────────────────────────────

    @Test
    @DisplayName("Buying additional shares updates weighted average cost correctly")
    void buy_updatesWeightedAverageCost() {
        // Existing: 10 shares @ $150 average
        Position existingPos = Position.builder()
                .account(account).symbol("AAPL").quantity(10).averageCost(new BigDecimal("150.00")).build();
        when(positionRepository.findByAccountIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(existingPos));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Buy 10 more @ current market $189.50
        engine.submitOrder(account, "AAPL", OrderSide.BUY, OrderType.MARKET, 10, null);

        // Expected avg = (10*150 + 10*189.50) / 20 = (1500 + 1895) / 20 = 169.75
        assertThat(existingPos.getAverageCost()).isEqualByComparingTo("169.7500");
        assertThat(existingPos.getQuantity()).isEqualTo(20);
    }
}
