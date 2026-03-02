package com.example.service_order.scheduler;

import com.example.service_order.entity.Order;
import com.example.service_order.entity.OrderStatus;
import com.example.service_order.event.OrderCancelledEvent;
import com.example.service_order.event.OrderCreatedEvent;
import com.example.service_order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReconciliationScheduler {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedRate = 300_000)
    public void reconcileStaleOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        reconcilePendingOrders(threshold);
        reconcileCancellingOrders(threshold);
    }

    private void reconcilePendingOrders(LocalDateTime threshold) {
        List<Order> staleOrders = orderRepository.findByStatusAndUpdatedAtBefore(OrderStatus.PENDING, threshold);
        for (Order order : staleOrders) {
            try {
                log.warn("[RECONCILE] PENDING 상태 장기 체류 주문 감지 → 이벤트 재발행 - orderId={}, itemId={}",
                        order.getSeq(), order.getItemId());
                OrderCreatedEvent event = new OrderCreatedEvent(order.getSeq(), order.getItemId());
                kafkaTemplate.send("order-events", String.valueOf(order.getSeq()), event);
            } catch (Exception e) {
                log.error("[RECONCILE] PENDING 주문 재발행 실패 - orderId={}, error={}", order.getSeq(), e.getMessage());
            }
        }
        if (!staleOrders.isEmpty()) {
            log.info("[RECONCILE] PENDING 상태 재처리 완료 - count={}", staleOrders.size());
        }
    }

    private void reconcileCancellingOrders(LocalDateTime threshold) {
        List<Order> staleOrders = orderRepository.findByStatusAndUpdatedAtBefore(OrderStatus.CANCELLING, threshold);
        for (Order order : staleOrders) {
            try {
                log.warn("[RECONCILE] CANCELLING 상태 장기 체류 주문 감지 → 이벤트 재발행 - orderId={}, itemId={}",
                        order.getSeq(), order.getItemId());
                OrderCancelledEvent event = new OrderCancelledEvent(order.getSeq(), order.getItemId());
                kafkaTemplate.send("order-events", String.valueOf(order.getSeq()), event);
            } catch (Exception e) {
                log.error("[RECONCILE] CANCELLING 주문 재발행 실패 - orderId={}, error={}", order.getSeq(), e.getMessage());
            }
        }
        if (!staleOrders.isEmpty()) {
            log.info("[RECONCILE] CANCELLING 상태 재처리 완료 - count={}", staleOrders.size());
        }
    }
}
