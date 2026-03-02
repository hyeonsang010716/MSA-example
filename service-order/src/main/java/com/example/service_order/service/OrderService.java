package com.example.service_order.service;

import com.example.service_order.dto.OrderResponse;
import com.example.service_order.entity.Order;
import com.example.service_order.entity.OrderStatus;
import com.example.service_order.event.OrderCancelledEvent;
import com.example.service_order.event.OrderCreatedEvent;
import com.example.service_order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public OrderResponse buy(Long itemId) {
        Order order = Order.builder()
                .itemId(itemId)
                .quantity(1)
                .status(OrderStatus.PENDING)
                .build();
        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(order.getSeq(), order.getItemId());
        kafkaTemplate.send("order-events", String.valueOf(order.getSeq()), event);
        log.info("[ORDER] 주문 생성 - orderId={}, itemId={}, status=PENDING", order.getSeq(), itemId);

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 주문만 취소할 수 있습니다.");
        }

        order.updateStatus(OrderStatus.CANCELLING);

        OrderCancelledEvent event = new OrderCancelledEvent(order.getSeq(), order.getItemId());
        kafkaTemplate.send("order-events", String.valueOf(order.getSeq()), event);
        log.info("[ORDER] 주문 취소 요청 - orderId={}, itemId={}, status=CANCELLING", orderId, order.getItemId());

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> search(Long orderId, Long itemId, OrderStatus status) {
        if (orderId != null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
            log.info("[ORDER] 주문 조회 - orderId={}", orderId);
            return List.of(OrderResponse.from(order));
        }

        List<Order> orders;
        if (itemId != null && status != null) {
            orders = orderRepository.findByItemIdAndStatus(itemId, status);
        } else if (itemId != null) {
            orders = orderRepository.findByItemId(itemId);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }

        log.info("[ORDER] 주문 검색 - itemId={}, status={}, 결과={}건", itemId, status, orders.size());
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public void updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        order.updateStatus(status);
        log.info("[ORDER] 주문 상태 변경 - orderId={}, status={}", orderId, status);
    }
}
