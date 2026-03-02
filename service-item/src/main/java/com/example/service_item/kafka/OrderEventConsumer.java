package com.example.service_item.kafka;

import com.example.service_item.event.*;
import com.example.service_item.exception.DuplicateEventException;
import com.example.service_item.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "order-events", groupId = "item-service-group")
public class OrderEventConsumer {

    private final ItemService itemService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[ITEM] 주문 생성 이벤트 수신 - orderId={}, itemId={}", event.getOrderId(), event.getItemId());
        try {
            itemService.buyForOrder(event.getOrderId(), event.getItemId());

            StockDecreasedEvent result = new StockDecreasedEvent(event.getOrderId(), event.getItemId());
            kafkaTemplate.send("item-events", String.valueOf(event.getOrderId()), result);
            log.info("[ITEM] 재고 차감 성공 - orderId={}, itemId={}", event.getOrderId(), event.getItemId());
        } catch (DuplicateEventException e) {
            log.warn("[ITEM] 중복 이벤트 무시 - orderId={}", event.getOrderId());
            throw e;
        } catch (IllegalArgumentException e) {
            StockDecreaseFailedEvent result = new StockDecreaseFailedEvent(
                    event.getOrderId(), event.getItemId(), e.getMessage());
            kafkaTemplate.send("item-events", String.valueOf(event.getOrderId()), result);
            log.warn("[ITEM] 재고 차감 실패 - orderId={}, itemId={}, reason={}",
                    event.getOrderId(), event.getItemId(), e.getMessage());
        }
    }

    @KafkaHandler
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("[ITEM] 주문 취소 이벤트 수신 - orderId={}, itemId={}", event.getOrderId(), event.getItemId());
        try {
            itemService.cancelForOrder(event.getOrderId(), event.getItemId());

            StockIncreasedEvent result = new StockIncreasedEvent(event.getOrderId(), event.getItemId());
            kafkaTemplate.send("item-events", String.valueOf(event.getOrderId()), result);
            log.info("[ITEM] 재고 복구 성공 - orderId={}, itemId={}", event.getOrderId(), event.getItemId());
        } catch (DuplicateEventException e) {
            log.warn("[ITEM] 중복 이벤트 무시 - orderId={}", event.getOrderId());
            throw e;
        }
    }

    @KafkaHandler
    public void handleCompensateStockDecrease(CompensateStockDecreaseEvent event) {
        log.info("[ITEM] 재고 보상 요청 수신 - orderId={}, itemId={}", event.getOrderId(), event.getItemId());
        try {
            itemService.compensateForOrder(event.getOrderId(), event.getItemId());

            StockDecreaseFailedEvent result = new StockDecreaseFailedEvent(
                    event.getOrderId(), event.getItemId(), "보상 트랜잭션으로 재고 복구됨");
            kafkaTemplate.send("item-events", String.valueOf(event.getOrderId()), result);
            log.info("[ITEM] 재고 보상 완료 - orderId={}, itemId={}", event.getOrderId(), event.getItemId());
        } catch (DuplicateEventException e) {
            log.warn("[ITEM] 중복 보상 이벤트 무시 - orderId={}", event.getOrderId());
            throw e;
        }
    }

    @KafkaHandler
    public void handleOrderStatusUpdateFailed(OrderStatusUpdateFailedEvent event) {
        log.warn("[ITEM] 주문 상태 업데이트 실패 알림 수신 - orderId={}, itemId={}", event.getOrderId(), event.getItemId());
    }
}
