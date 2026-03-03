package com.example.service_item.controller;

import com.example.service_item.dto.ApiResponse;
import com.example.service_item.dto.ItemRegisterRequest;
import com.example.service_item.dto.ItemResponse;
import com.example.service_item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody ItemRegisterRequest request) {
        itemService.register(request);
        return ResponseEntity.ok(ApiResponse.success("아이템 등록이 완료되었습니다."));
    }

    @Operation(summary = "전체 상품 조회", description = "등록된 모든 상품 목록을 조회합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> searchAll() {
        List<ItemResponse> items = itemService.searchAll();
        return ResponseEntity.ok(ApiResponse.success("아이템 조회가 완료되었습니다.", items));
    }

    @Operation(summary = "상품 구매 (재고 차감)", description = "상품의 재고를 1개 차감합니다.")
    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<Void>> buy(@RequestParam("item_id") Long itemId) {
        itemService.buy(itemId);
        return ResponseEntity.ok(ApiResponse.success("아이템 구매가 완료되었습니다."));
    }

    @Operation(summary = "구매 취소 (재고 복구)", description = "상품의 재고를 1개 복구합니다.")
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@RequestParam("item_id") Long itemId) {
        itemService.cancel(itemId);
        return ResponseEntity.ok(ApiResponse.success("아이템 구매 취소가 완료되었습니다."));
    }
}
