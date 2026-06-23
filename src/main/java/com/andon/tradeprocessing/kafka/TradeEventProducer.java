package com.andon.tradeprocessing.kafka;

import com.andon.tradeprocessing.dto.TradeExecutedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class TradeEventProducer {

    private final KafkaTemplate<String, TradeExecutedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.trade-executed}")
    private String tradeExecutedTopic;

    @Autowired
    public TradeEventProducer(@Autowired(required = false) KafkaTemplate<String, TradeExecutedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a TradeExecutedEvent to Kafka if Kafka is available; otherwise
     * no-op.
     */
    public void publishTradeExecuted(TradeExecutedEvent event) {
        if (kafkaTemplate == null) {
            log.debug("KafkaTemplate not available - skipping publish for tradeId={}", event.getTradeId());
            return;
        }

        CompletableFuture<SendResult<String, TradeExecutedEvent>> future = kafkaTemplate.send(tradeExecutedTopic,
                event.getSymbol(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish TradeExecutedEvent for tradeId={} symbol={}: {}",
                        event.getTradeId(), event.getSymbol(), ex.getMessage());
            } else {
                log.info("Published TradeExecutedEvent: tradeId={} symbol={} side={} qty={} price={} partition={}",
                        event.getTradeId(), event.getSymbol(), event.getSide(),
                        event.getQuantity(), event.getExecutionPrice(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}
