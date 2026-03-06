package com.bsknblc.inventory_service.service;

import com.bsknblc.inventory_service.dto.InventoryResponse;
import com.bsknblc.inventory_service.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j //for logging
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    @SneakyThrows //don't use this in production, it will consume the expection
    public List<InventoryResponse> isInStock(List<String> skuCode) {
//        log.info("Wait started");
//        Thread.sleep(10000); //simulates network delay
//        log.info("Wait ended");
        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(inventory ->
                    InventoryResponse.builder()
                            .skuCode(inventory.getSkuCode())
                            .isInStock(inventory.getQuantity() > 0)
                            .build()
                ).toList();
    }

}
