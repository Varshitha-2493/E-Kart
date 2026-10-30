package com.ekart.ekart.controller;

import com.ekart.ekart.config.AdminGuard;
import com.ekart.ekart.dto.CheckoutRequest;
import com.ekart.ekart.dto.OrderItemResponse;
import com.ekart.ekart.dto.OrderResponse;
import com.ekart.ekart.dto.OrderStatusUpdateRequest;
import com.ekart.ekart.exception.ApiException;
import com.ekart.ekart.model.Order;
import com.ekart.ekart.model.User;
import com.ekart.ekart.repository.OrderRepository;
import com.ekart.ekart.repository.UserRepository;
import com.ekart.ekart.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final AdminGuard adminGuard;

    public OrderController(OrderRepository orderRepository,
                           UserRepository userRepository,
                           OrderService orderService,
                           AdminGuard adminGuard) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.adminGuard = adminGuard;
    }

    @PostMapping("/{userId}/checkout")
    public OrderResponse checkout(
            @PathVariable Long userId,
            @RequestBody CheckoutRequest request) {

        Order order = orderService.checkout(userId, request.shippingAddress());
        return convertToResponse(order);
    }

    @GetMapping("/{userId}")
    public List<OrderResponse> getOrderHistory(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        return orderRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @GetMapping("/detail/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        return convertToResponse(order);
    }

    @GetMapping("/admin/all")
    public List<OrderResponse> getAllOrders(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {

        adminGuard.require(adminId);
        return orderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::convertToResponse)
                .toList();
    }

    @PutMapping("/admin/{orderId}/status")
    public OrderResponse updateStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request) {

        adminGuard.require(adminId);
        if (request.status() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        return convertToResponse(orderService.updateStatus(orderId, request.status()));
    }

    private OrderResponse convertToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    BigDecimal total = item.getPriceAtPurchase()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
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
