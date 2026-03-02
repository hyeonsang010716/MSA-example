package com.example.service_item.config;

import com.example.service_item.event.CancelProcessingFailedEvent;
import com.example.service_item.event.OrderCancelledEvent;
import com.example.service_item.event.OrderCreatedEvent;
import com.example.service_item.event.OrderProcessingFailedEvent;
import com.example.service_item.exception.DuplicateEventException;
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
                    log.error("[ITEM-DLT] Dead Letter 처리 - topic={}, key={}, error={}",
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
            if (value instanceof OrderCreatedEvent event) {
                log.warn("[ITEM] OrderCreatedEvent 처리 실패 → OrderProcessingFailedEvent 발행 - orderId={}", event.getOrderId());
                OrderProcessingFailedEvent failedEvent = new OrderProcessingFailedEvent(
                        event.getOrderId(), event.getItemId(), exception.getMessage());
                kafkaTemplate.send("item-events", String.valueOf(event.getOrderId()), failedEvent);
            } else if (value instanceof OrderCancelledEvent event) {
                log.warn("[ITEM] OrderCancelledEvent 처리 실패 → CancelProcessingFailedEvent 발행 - orderId={}", event.getOrderId());
                CancelProcessingFailedEvent failedEvent = new CancelProcessingFailedEvent(
                        event.getOrderId(), event.getItemId(), exception.getMessage());
                kafkaTemplate.send("item-events", String.valueOf(event.getOrderId()), failedEvent);
            }
        } catch (Exception e) {
            log.error("[ITEM] 보상 이벤트 발행 실패 - record={}, error={}", record.key(), e.getMessage());
        }
    }
}
