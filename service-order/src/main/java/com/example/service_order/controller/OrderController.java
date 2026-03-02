package com.example.service_order.controller;

import com.example.service_order.dto.ApiResponse;
import com.example.service_order.dto.OrderResponse;
import com.example.service_order.entity.OrderStatus;
import com.example.service_order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<OrderResponse>> buy(@RequestParam("item_id") Long itemId) {
        OrderResponse response = orderService.buy(itemId);
        return ResponseEntity.ok(ApiResponse.success(response.getStatus().getMessage(), response));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@RequestParam("order_id") Long orderId) {
        OrderResponse response = orderService.cancel(orderId);
        return ResponseEntity.ok(ApiResponse.success(response.getStatus().getMessage(), response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> search(
            @RequestParam(value = "order_id", required = false) Long orderId,
            @RequestParam(value = "item_id", required = false) Long itemId,
            @RequestParam(value = "status", required = false) OrderStatus status) {
        List<OrderResponse> responses = orderService.search(orderId, itemId, status);
        return ResponseEntity.ok(ApiResponse.success("주문 조회 성공", responses));
    }
}
