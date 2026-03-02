package com.example.service_order.exception;

public class DuplicateEventException extends RuntimeException {

    public DuplicateEventException(String eventKey) {
        super("이미 처리된 이벤트입니다: " + eventKey);
    }
}
