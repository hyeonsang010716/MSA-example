package com.example.service_item.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ItemRegisterRequest {

    private String name;
    private int price;
    private int quantity;
}
