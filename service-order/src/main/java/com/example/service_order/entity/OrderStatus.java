package com.example.service_order.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상태", enumAsRef = true)
public enum OrderStatus {
    PENDING("주문이 접수되었습니다. 처리 중입니다."),
    COMPLETED("주문이 완료되었습니다."),
    FAILED("주문 처리에 실패했습니다."),
    CANCELLING("주문 취소가 요청되었습니다. 처리 중입니다."),
    CANCELLED("주문이 취소되었습니다."),
    CANCEL_FAILED("주문 취소 처리에 실패했습니다.");

    private final String message;

    OrderStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
