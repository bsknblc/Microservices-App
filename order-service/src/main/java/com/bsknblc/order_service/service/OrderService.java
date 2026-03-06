package com.bsknblc.order_service.service;

import com.bsknblc.order_service.dto.InventoryResponse;
import com.bsknblc.order_service.dto.OrderLineItemsDTO;
import com.bsknblc.order_service.dto.OrderRequest;
import com.bsknblc.order_service.model.Order;
import com.bsknblc.order_service.model.OrderLineItems;
import com.bsknblc.order_service.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDTOS()
                .stream()
                .map(this::mapToDTO)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                return "Order placed successfully";
            }else{
                throw new IllegalArgumentException("Product is not in stock!");
            }
        } finally {
            inventoryServiceLookup.end();
        }

    }

    private OrderLineItems mapToDTO(OrderLineItemsDTO orderLineItemsDTO) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setSkuCode(orderLineItemsDTO.getSkuCode());
        orderLineItems.setQuantity(orderLineItemsDTO.getQuantity());
        orderLineItems.setPrice(orderLineItemsDTO.getPrice());
        return orderLineItems;
    }
}
