package com.example.service_item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ItemRegisterRequest {

    @Schema(description = "상품명", example = "테스트상품")
    private String name;

    @Schema(description = "가격", example = "10000")
    private int price;

    @Schema(description = "재고 수량", example = "100")
    private int quantity;
}
