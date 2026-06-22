package com.andon.tradeprocessing.kafka;

import com.andon.tradeprocessing.dto.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventProducer {

    private final KafkaTemplate<String, TradeExecutedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.trade-executed}")
    private String tradeExecutedTopic;

    /**
     * Publishes a TradeExecutedEvent to Kafka.
     * Key = symbol so all trades for the same symbol land on the same partition (ordering guarantee).
     */
    public void publishTradeExecuted(TradeExecutedEvent event) {
        CompletableFuture<SendResult<String, TradeExecutedEvent>> future =
                kafkaTemplate.send(tradeExecutedTopic, event.getSymbol(), event);

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
