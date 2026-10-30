package com.ekart.ekart.controller;

import com.ekart.ekart.config.AdminGuard;
import com.ekart.ekart.exception.ApiException;
import com.ekart.ekart.model.Category;
import com.ekart.ekart.model.Product;
import com.ekart.ekart.repository.CategoryRepository;
import com.ekart.ekart.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AdminGuard adminGuard;

    public ProductController(ProductRepository productRepository,
                             CategoryRepository categoryRepository,
                             AdminGuard adminGuard) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.adminGuard = adminGuard;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/filter")
    public List<Product> filterProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "false") boolean inStockOnly) {

        String query = (q == null || q.isBlank()) ? null : q.trim();
        return productRepository.filter(
                categoryId, query, minPrice, maxPrice, minRating, inStockOnly
        );
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(@PathVariable Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @PostMapping
    public Product addProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestBody Product product) {

        adminGuard.require(adminId);
        resolveCategory(product);
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public Product updateProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @PathVariable Long id,
            @RequestBody Product updatedProduct) {

        adminGuard.require(adminId);
        return applyUpdate(id, updatedProduct);
    }

    @PutMapping("/admin/{id}")
    public Product updateProductAsAdmin(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @PathVariable Long id,
            @RequestBody Product updatedProduct) {

        adminGuard.require(adminId);
        return applyUpdate(id, updatedProduct);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @PathVariable Long id) {

        adminGuard.require(adminId);
        if (!productRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
        return "Product deleted successfully";
    }

    private Product applyUpdate(Long id, Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setSpecs(updatedProduct.getSpecs());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setImageUrl(updatedProduct.getImageUrl());
        if (updatedProduct.getAverageRating() != null) {
            product.setAverageRating(updatedProduct.getAverageRating());
        }
        if (updatedProduct.getCategory() != null) {
            resolveCategory(updatedProduct);
            product.setCategory(updatedProduct.getCategory());
        }
        return productRepository.save(product);
    }

    private void resolveCategory(Product product) {
        if (product.getCategory() == null || product.getCategory().getId() == null) {
            return;
        }
        Category category = categoryRepository.findById(product.getCategory().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found"));
        product.setCategory(category);
    }
}
