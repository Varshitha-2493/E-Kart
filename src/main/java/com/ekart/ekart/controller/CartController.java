package com.ekart.ekart.controller;

import java.util.List;
import com.ekart.ekart.dto.CartItemResponse;
import com.ekart.ekart.model.CartItem;
import com.ekart.ekart.model.Product;
import com.ekart.ekart.model.User;
import com.ekart.ekart.repository.CartItemRepository;
import com.ekart.ekart.repository.ProductRepository;
import com.ekart.ekart.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartController(
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {

        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @PostMapping("/add")
    public CartItemResponse addToCart(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (quantity > product.getStock()) {
            throw new RuntimeException("Not enough stock available");
        }
        CartItem existingItem = cartItemRepository
                .findByUserIdAndProductId(userId, productId)
                .orElse(null);

        CartItem cartItem;

        if (existingItem != null) {

            existingItem.setQuantity(
                    existingItem.getQuantity() + quantity
            );

            cartItem = cartItemRepository.save(existingItem);

        } else {

            cartItem = new CartItem();

            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);

            cartItem = cartItemRepository.save(cartItem);
        }

        BigDecimal total = cartItem.getProduct()
                .getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                cartItem.getQuantity(),
                total
        );
    }
    @GetMapping("/{userId}")
    public List<CartItemResponse> getCart(@PathVariable Long userId) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        return cartItems.stream()
                .map(item -> {

                    BigDecimal total = item.getProduct()
                            .getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    return new CartItemResponse(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getProduct().getPrice(),
                            item.getQuantity(),
                            total
                    );
                })
                .toList();
    }
    @DeleteMapping("/{userId}/{productId}")
    public String removeFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {

        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        cartItemRepository.delete(cartItem);

        return "Product removed from cart";
    }
    @PutMapping("/{userId}/{productId}")
    public CartItemResponse updateQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        if (quantity > cartItem.getProduct().getStock()) {
            throw new RuntimeException("Not enough stock available");
        }

        cartItem.setQuantity(quantity);

        cartItem = cartItemRepository.save(cartItem);

        BigDecimal total = cartItem.getProduct()
                .getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity(),
                total
        );
    }
}