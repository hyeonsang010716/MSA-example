package com.example.service_item.controller;

import com.example.service_item.dto.ApiResponse;
import com.example.service_item.dto.ItemRegisterRequest;
import com.example.service_item.dto.ItemResponse;
import com.example.service_item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody ItemRegisterRequest request) {
        itemService.register(request);
        return ResponseEntity.ok(ApiResponse.success("아이템 등록이 완료되었습니다."));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> searchAll() {
        List<ItemResponse> items = itemService.searchAll();
        return ResponseEntity.ok(ApiResponse.success("아이템 조회가 완료되었습니다.", items));
    }
}
