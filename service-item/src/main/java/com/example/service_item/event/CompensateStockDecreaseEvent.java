package com.example.service_item.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompensateStockDecreaseEvent {
    private Long orderId;
    private Long itemId;
}
