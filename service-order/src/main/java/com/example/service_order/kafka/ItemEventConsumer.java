package com.example.service_order.kafka;

import com.example.service_order.entity.OrderStatus;
import com.example.service_order.entity.ProcessedEvent;
import com.example.service_order.event.*;
import com.example.service_order.exception.DuplicateEventException;
import com.example.service_order.repository.ProcessedEventRepository;
import com.example.service_order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "item-events", groupId = "order-service-group")
public class ItemEventConsumer {

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaHandler
    @Transactional
    public void handleStockDecreased(StockDecreasedEvent event) {
        // [TEST] itemId=2이면 강제 에러 → 재고 차감 성공 후 주문 상태 변경 실패 → 보상 트랜잭션 테스트
        if (event.getItemId() == 2L) {
            throw new RuntimeException("[TEST] itemId=2 강제 에러 - 주문 COMPLETED 업데이트 실패 보상 테스트");
        }

        String eventKey = "StockDecreased:" + event.getOrderId();
        checkDuplicate(eventKey);

        log.info("[ORDER] 재고 차감 성공 이벤트 수신 - orderId={}", event.getOrderId());
        orderService.updateStatus(event.getOrderId(), OrderStatus.COMPLETED);
        processedEventRepository.save(new ProcessedEvent(eventKey));
    }

    @KafkaHandler
    @Transactional
    public void handleStockDecreaseFailed(StockDecreaseFailedEvent event) {
        String eventKey = "StockDecreaseFailed:" + event.getOrderId();
        checkDuplicate(eventKey);

        log.warn("[ORDER] 재고 차감 실패 이벤트 수신 - orderId={}, reason={}", event.getOrderId(), event.getReason());
        orderService.updateStatus(event.getOrderId(), OrderStatus.FAILED);
        processedEventRepository.save(new ProcessedEvent(eventKey));
    }

    @KafkaHandler
    @Transactional
    public void handleStockIncreased(StockIncreasedEvent event) {
        String eventKey = "StockIncreased:" + event.getOrderId();
        checkDuplicate(eventKey);

        log.info("[ORDER] 재고 복구 성공 이벤트 수신 - orderId={}", event.getOrderId());
        orderService.updateStatus(event.getOrderId(), OrderStatus.CANCELLED);
        processedEventRepository.save(new ProcessedEvent(eventKey));
    }

    @KafkaHandler
    @Transactional
    public void handleOrderProcessingFailed(OrderProcessingFailedEvent event) {
        String eventKey = "OrderProcessingFailed:" + event.getOrderId();
        checkDuplicate(eventKey);

        log.warn("[ORDER] 주문 처리 실패 이벤트 수신 - orderId={}, reason={}", event.getOrderId(), event.getReason());
        orderService.updateStatus(event.getOrderId(), OrderStatus.FAILED);
        processedEventRepository.save(new ProcessedEvent(eventKey));
    }

    @KafkaHandler
    @Transactional
    public void handleCancelProcessingFailed(CancelProcessingFailedEvent event) {
        String eventKey = "CancelProcessingFailed:" + event.getOrderId();
        checkDuplicate(eventKey);

        log.warn("[ORDER] 취소 처리 실패 이벤트 수신 - orderId={}, reason={}", event.getOrderId(), event.getReason());
        orderService.updateStatus(event.getOrderId(), OrderStatus.CANCEL_FAILED);
        processedEventRepository.save(new ProcessedEvent(eventKey));
    }

    private void checkDuplicate(String eventKey) {
        if (processedEventRepository.existsByEventKey(eventKey)) {
            log.warn("[ORDER] 중복 이벤트 무시 - eventKey={}", eventKey);
            throw new DuplicateEventException(eventKey);
        }
    }
}
