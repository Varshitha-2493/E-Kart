package com.ekart.ekart.controller;

import com.ekart.ekart.dto.ReviewRequest;
import com.ekart.ekart.dto.ReviewResponse;
import com.ekart.ekart.exception.ApiException;
import com.ekart.ekart.model.Product;
import com.ekart.ekart.model.Review;
import com.ekart.ekart.model.User;
import com.ekart.ekart.repository.ProductRepository;
import com.ekart.ekart.repository.ReviewRepository;
import com.ekart.ekart.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewController(ReviewRepository reviewRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/product/{productId}")
    public List<ReviewResponse> listForProduct(@PathVariable Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/product/{productId}")
    public ReviewResponse addReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequest request) {

        if (request.userId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rating must be 1 to 5");
        }
        if (request.comment() == null || request.comment().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Comment is required");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment().trim());

        Review saved = reviewRepository.save(review);
        refreshAverageRating(productId);
        return toResponse(saved);
    }

    private void refreshAverageRating(Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        if (reviews.isEmpty()) {
            return;
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setAverageRating(Math.round(avg * 10.0) / 10.0);
            productRepository.save(product);
        }
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
