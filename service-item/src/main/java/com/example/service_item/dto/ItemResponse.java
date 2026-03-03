package com.example.service_item.dto;

import com.example.service_item.entity.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long seq;

    @Schema(description = "상품명", example = "테스트상품")
    private String name;

    @Schema(description = "가격", example = "10000")
    private int price;

    @Schema(description = "재고 수량", example = "100")
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
