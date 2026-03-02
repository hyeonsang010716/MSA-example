package com.example.service_order.config;

import com.example.service_order.event.CompensateStockDecreaseEvent;
import com.example.service_order.event.OrderStatusUpdateFailedEvent;
import com.example.service_order.event.StockDecreasedEvent;
import com.example.service_order.event.StockIncreasedEvent;
import com.example.service_order.exception.DuplicateEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("[ORDER-DLT] Dead Letter 처리 - topic={}, key={}, error={}",
                            record.topic(), record.key(), exception.getMessage());
                    publishCompensationEvent(record, exception);
                },
                new FixedBackOff(1000L, 3)
        );
        errorHandler.addNotRetryableExceptions(DuplicateEventException.class);
        return errorHandler;
    }

    private void publishCompensationEvent(ConsumerRecord<?, ?> record, Exception exception) {
        Object value = record.value();
        try {
            if (value instanceof StockDecreasedEvent event) {
                log.warn("[ORDER] StockDecreasedEvent 처리 실패 → 보상 이벤트 발행 - orderId={}", event.getOrderId());
                CompensateStockDecreaseEvent compensateEvent =
                        new CompensateStockDecreaseEvent(event.getOrderId(), event.getItemId());
                kafkaTemplate.send("order-events", String.valueOf(event.getOrderId()), compensateEvent);
            } else if (value instanceof StockIncreasedEvent event) {
                log.warn("[ORDER] StockIncreasedEvent 처리 실패 → 로그 경고 (재고는 안전) - orderId={}, Scheduler가 재처리 예정",
                        event.getOrderId());
                OrderStatusUpdateFailedEvent failedEvent =
                        new OrderStatusUpdateFailedEvent(event.getOrderId(), event.getItemId());
                kafkaTemplate.send("order-events", String.valueOf(event.getOrderId()), failedEvent);
            }
        } catch (Exception e) {
            log.error("[ORDER] 보상 이벤트 발행 실패 - record={}, error={}", record.key(), e.getMessage());
        }
    }
}
