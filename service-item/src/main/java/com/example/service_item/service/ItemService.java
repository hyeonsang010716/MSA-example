package com.example.service_item.service;

import com.example.service_item.dto.ItemRegisterRequest;
import com.example.service_item.dto.ItemResponse;
import com.example.service_item.entity.Item;
import com.example.service_item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void register(ItemRegisterRequest request) {
        Item item = Item.builder()
                .name(request.getName())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .build();

        itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> searchAll() {
        return itemRepository.findAll().stream()
                .map(ItemResponse::from)
                .toList();
    }
}
