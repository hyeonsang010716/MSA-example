package com.example.service_order.repository;

import com.example.service_order.entity.Order;
import com.example.service_order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime before);

    List<Order> findByItemId(Long itemId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByItemIdAndStatus(Long itemId, OrderStatus status);
}
