package com.example.service_item.service;

import com.example.service_item.dto.ItemRegisterRequest;
import com.example.service_item.dto.ItemResponse;
import com.example.service_item.entity.Item;
import com.example.service_item.entity.ProcessedEvent;
import com.example.service_item.exception.DuplicateEventException;
import com.example.service_item.repository.ItemRepository;
import com.example.service_item.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ProcessedEventRepository processedEventRepository;

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

    @Transactional
    public void buy(Long itemId) {
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다."));
        item.decreaseQuantity();
    }

    @Transactional
    public void cancel(Long itemId) {
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다."));
        item.increaseQuantity();
    }

    @Transactional
    public void buyForOrder(Long orderId, Long itemId) {
        // [TEST] itemId=1이면 강제 에러 → 보상 트랜잭션 테스트용
        if (itemId == 1L) {
            throw new RuntimeException("[TEST] itemId=1 강제 에러 - 보상 트랜잭션 테스트");
        }

        String eventKey = "OrderCreated:" + orderId;
        if (processedEventRepository.existsByEventKey(eventKey)) {
            throw new DuplicateEventException(eventKey);
        }

        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다."));
        item.decreaseQuantity();
        processedEventRepository.save(new ProcessedEvent(eventKey));
        log.info("[ITEM] Saga 재고 차감 - orderId={}, itemId={}", orderId, itemId);
    }

    @Transactional
    public void cancelForOrder(Long orderId, Long itemId) {
        String eventKey = "OrderCancelled:" + orderId;
        if (processedEventRepository.existsByEventKey(eventKey)) {
            throw new DuplicateEventException(eventKey);
        }

        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다."));
        item.increaseQuantity();
        processedEventRepository.save(new ProcessedEvent(eventKey));
        log.info("[ITEM] Saga 재고 복구 - orderId={}, itemId={}", orderId, itemId);
    }

    @Transactional
    public void compensateForOrder(Long orderId, Long itemId) {
        String eventKey = "CompensateStockDecrease:" + orderId;
        if (processedEventRepository.existsByEventKey(eventKey)) {
            throw new DuplicateEventException(eventKey);
        }

        String originalEventKey = "OrderCreated:" + orderId;
        if (!processedEventRepository.existsByEventKey(originalEventKey)) {
            log.warn("[ITEM] 보상 대상 없음 (원본 차감 기록 없음) - orderId={}", orderId);
            return;
        }

        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다."));
        item.increaseQuantity();
        processedEventRepository.save(new ProcessedEvent(eventKey));
        log.info("[ITEM] 보상 트랜잭션 재고 복구 - orderId={}, itemId={}", orderId, itemId);
    }
}
