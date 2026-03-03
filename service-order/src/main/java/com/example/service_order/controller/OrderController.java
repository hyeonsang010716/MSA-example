package com.example.service_order.controller;

import com.example.service_order.dto.ApiResponse;
import com.example.service_order.dto.OrderResponse;
import com.example.service_order.entity.OrderStatus;
import com.example.service_order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "주문 생성", description = "상품 주문을 생성하고 Saga 흐름을 시작합니다.")
    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<OrderResponse>> buy(@RequestParam("item_id") Long itemId) {
        OrderResponse response = orderService.buy(itemId);
        return ResponseEntity.ok(ApiResponse.success(response.getStatus().getMessage(), response));
    }

    @Operation(summary = "주문 취소", description = "완료된 주문을 취소하고 보상 트랜잭션을 시작합니다.")
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@RequestParam("order_id") Long orderId) {
        OrderResponse response = orderService.cancel(orderId);
        return ResponseEntity.ok(ApiResponse.success(response.getStatus().getMessage(), response));
    }

    @Operation(summary = "주문 조회", description = "주문 ID, 상품 ID, 주문 상태로 필터링하여 조회합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> search(
            @RequestParam(value = "order_id", required = false) Long orderId,
            @RequestParam(value = "item_id", required = false) Long itemId,
            @RequestParam(value = "status", required = false) OrderStatus status) {
        List<OrderResponse> responses = orderService.search(orderId, itemId, status);
        return ResponseEntity.ok(ApiResponse.success("주문 조회 성공", responses));
    }
}
