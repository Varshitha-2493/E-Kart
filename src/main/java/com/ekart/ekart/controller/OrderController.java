package com.ekart.ekart.controller;

import com.ekart.ekart.dto.CheckoutRequest;
import com.ekart.ekart.dto.OrderItemResponse;
import com.ekart.ekart.dto.OrderResponse;
import com.ekart.ekart.model.Order;
import com.ekart.ekart.model.OrderItem;
import com.ekart.ekart.model.User;
import com.ekart.ekart.repository.OrderRepository;
import com.ekart.ekart.repository.UserRepository;
import com.ekart.ekart.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    public OrderController(OrderRepository orderRepository,
                           UserRepository userRepository,
                           OrderService orderService) {

        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    @PostMapping("/{userId}/checkout")
    public OrderResponse checkout(
            @PathVariable Long userId,
            @RequestBody CheckoutRequest request) {

        Order order = orderService.checkout(
                userId,
                request.shippingAddress()
        );

        return convertToResponse(order);
    }

    @GetMapping("/{userId}")
    public List<OrderResponse> getOrderHistory(
            @PathVariable Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return orderRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private OrderResponse convertToResponse(Order order) {

        List<OrderItemResponse> itemResponses =
                order.getItems()
                        .stream()
                        .map(item -> {

                            BigDecimal total =
                                    item.getPriceAtPurchase()
                                            .multiply(
                                                    BigDecimal.valueOf(
                                                            item.getQuantity()
                                                    )
                                            );

                            return new OrderItemResponse(
                                    item.getProduct().getId(),
                                    item.getProductName(),
                                    item.getQuantity(),
                                    item.getPriceAtPurchase(),
                                    total
                            );
                        })
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getDeliveryDate(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}