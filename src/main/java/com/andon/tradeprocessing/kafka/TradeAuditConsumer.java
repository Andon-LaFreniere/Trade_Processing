package com.andon.tradeprocessing.kafka;

import com.andon.tradeprocessing.dto.TradeExecutedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Audit consumer: subscribes to trade-executed events and logs them.
 *
 * In a real financial system, this consumer would:
 *   - Write to a separate audit database (regulatory requirement)
 *   - Feed a risk management system
 *   - Trigger downstream settlement workflows
 *   - Emit metrics to a monitoring platform (Prometheus, Datadog)
 */
@Slf4j
@Component
public class TradeAuditConsumer {

    @KafkaListener(
            topics = "${app.kafka.topic.trade-executed}",
            groupId = "trade-audit-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTradeExecuted(
            @Payload TradeExecutedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("""
                [AUDIT] Trade settled | tradeId={} accountId={} symbol={} side={} qty={} price={} total={} executedAt={} | partition={} offset={}
                """.strip(),
                event.getTradeId(),
                event.getAccountId(),
                event.getSymbol(),
                event.getSide(),
                event.getQuantity(),
                event.getExecutionPrice(),
                event.getTotalValue(),
                event.getExecutedAt(),
                partition,
                offset
        );

        // Flag large trades for compliance review
        BigDecimal largeTradeThreshold = new BigDecimal("100000.00");
        if (event.getTotalValue().compareTo(largeTradeThreshold) > 0) {
            log.warn("[COMPLIANCE] Large trade detected: tradeId={} accountId={} totalValue={}",
                    event.getTradeId(), event.getAccountId(), event.getTotalValue());
        }
    }
}
