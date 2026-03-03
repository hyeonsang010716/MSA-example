package com.example.service_order.dto;

import com.example.service_order.entity.Order;
import com.example.service_order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long seq;

    @Schema(description = "상품 ID", example = "1")
    private Long itemId;

    @Schema(description = "주문 수량", example = "1")
    private int quantity;

    @Schema(description = "주문 상태", example = "COMPLETED")
    private OrderStatus status;

    @Schema(description = "주문 생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "최종 수정일시")
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getSeq(),
                order.getItemId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
