package com.example.service_order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockDecreasedEvent {

    private Long orderId;
    private Long itemId;
}
