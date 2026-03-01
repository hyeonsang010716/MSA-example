package com.example.service_item.dto;

import com.example.service_item.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemResponse {

    private Long seq;
    private String name;
    private int price;
    private int quantity;

    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getSeq(),
                item.getName(),
                item.getPrice(),
                item.getQuantity()
        );
    }
}
