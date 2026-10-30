package com.ekart.ekart.service;

import com.ekart.ekart.ai.GrokClient;
import com.ekart.ekart.dto.*;
import com.ekart.ekart.exception.ApiException;
import com.ekart.ekart.model.*;
import com.ekart.ekart.repository.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    private final GrokClient grokClient;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AiAssistantService(GrokClient grokClient,
                              ProductRepository productRepository,
                              ReviewRepository reviewRepository,
                              OrderRepository orderRepository,
                              CartItemRepository cartItemRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper) {
        this.grokClient = grokClient;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GoalToCartResponse goalToCart(GoalToCartRequest request) {
        if (request.goal() == null || request.goal().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal is required");
        }
        if (request.budget() == null || request.budget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Budget must be greater than 0");
        }

        List<Product> catalog = productRepository.findByStockGreaterThan(0);
        if (catalog.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No in-stock products available");
        }

        String catalogText = catalog.stream()
                .map(p -> String.format(
                        "id=%d | %s | category=%s | price=%.2f | stock=%d | specs=%s | desc=%s",
                        p.getId(),
                        p.getName(),
                        p.getCategory() != null ? p.getCategory().getName() : "N/A",
                        p.getPrice(),
                        p.getStock(),
                        nullToEmpty(p.getSpecs()),
                        abbreviate(nullToEmpty(p.getDescription()), 120)
                ))
                .collect(Collectors.joining("\n"));

        String system = """
                You are E-Kart's shopping planner. Suggest a cart ONLY using product ids from the catalog.
                Respect the budget in INR. Prefer fewer useful items over padding.
                Reply with JSON only (no markdown):
                {"items":[{"productId":1,"quantity":1,"reason":"..."}],"notes":"short explanation"}
                """;

        String userPrompt = "Goal: " + request.goal().trim()
                + "\nBudget (INR): " + request.budget()
                + "\nCatalog:\n" + catalogText;

        String raw = grokClient.chat(system, userPrompt);
        JsonNode root = parseJson(raw);

        Map<Long, Product> byId = catalog.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        List<GoalCartItemDto> items = new ArrayList<>();
        List<String> validationNotes = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        JsonNode itemsNode = root.path("items");
        if (!itemsNode.isArray() || itemsNode.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI did not return any cart items");
        }

        for (JsonNode node : itemsNode) {
            long productId = node.path("productId").asLong(-1);
            int quantity = node.path("quantity").asInt(0);
            String reason = node.path("reason").asText("Suggested for your goal");

            Product product = byId.get(productId);
            if (product == null) {
                validationNotes.add("Skipped unknown/out-of-catalog product id " + productId);
                continue;
            }
            if (quantity <= 0) {
                validationNotes.add("Skipped " + product.getName() + " — invalid quantity");
                continue;
            }
            if (quantity > product.getStock()) {
                validationNotes.add(
                        product.getName() + " quantity reduced from " + quantity
                                + " to stock " + product.getStock()
                );
                quantity = product.getStock();
            }

            BigDecimal line = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            if (total.add(line).compareTo(request.budget()) > 0) {
                validationNotes.add("Skipped " + product.getName() + " — would exceed budget");
                continue;
            }

            total = total.add(line);
            items.add(new GoalCartItemDto(
                    product.getId(),
                    product.getName(),
                    quantity,
                    product.getPrice(),
                    line,
                    reason,
                    true
            ));
        }

        if (items.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Could not build a cart within budget from available stock"
            );
        }

        boolean added = false;
        if (request.addToCart()) {
            if (request.userId() == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Login required to add AI cart");
            }
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
            for (GoalCartItemDto item : items) {
                mergeIntoCart(user, item.productId(), item.quantity());
            }
            added = true;
        }

        String notes = root.path("notes").asText("Cart built from your goal and budget.");
        return new GoalToCartResponse(
                request.goal().trim(),
                request.budget(),
                total.setScale(2, RoundingMode.HALF_UP),
                total.compareTo(request.budget()) <= 0,
                added,
                notes,
                items,
                validationNotes
        );
    }

    public CompareResponse compare(CompareRequest request) {
        if (request.productIds() == null
                || request.productIds().size() < 2
                || request.productIds().size() > 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select 2 or 3 products to compare");
        }
        if (request.preference() == null || request.preference().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Preference is required");
        }

        List<Long> uniqueIds = request.productIds().stream().distinct().toList();
        List<Product> products = productRepository.findAllById(uniqueIds);
        if (products.size() != uniqueIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more products were not found in the database");
        }

        String productBlock = products.stream()
                .map(p -> String.format(
                        "id=%d | name=%s | price=%.2f | rating=%s | stock=%d | specs=%s | description=%s",
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getAverageRating() == null ? "N/A" : p.getAverageRating(),
                        p.getStock(),
                        nullToEmpty(p.getSpecs()),
                        nullToEmpty(p.getDescription())
                ))
                .collect(Collectors.joining("\n"));

        String system = """
                Compare ONLY the given E-Kart products for the user's preference.
                Reply with JSON only:
                {"recommendedProductId":1,"reasoning":"...","comparisonPoints":["...","..."]}
                recommendedProductId must be one of the given ids.
                """;

        String user = "Preference: " + request.preference().trim() + "\nProducts:\n" + productBlock;
        JsonNode root = parseJson(grokClient.chat(system, user));

        long recommendedId = root.path("recommendedProductId").asLong(-1);
        Product recommended = products.stream()
                .filter(p -> Objects.equals(p.getId(), recommendedId))
                .findFirst()
                .orElse(null);

        if (recommended == null) {
            recommended = products.stream()
                    .min(Comparator.comparing(Product::getPrice))
                    .orElse(products.get(0));
        }

        List<String> points = new ArrayList<>();
        JsonNode pointsNode = root.path("comparisonPoints");
        if (pointsNode.isArray()) {
            pointsNode.forEach(n -> points.add(n.asText()));
        }

        return new CompareResponse(
                recommended.getId(),
                recommended.getName(),
                request.preference().trim(),
                root.path("reasoning").asText("Recommended based on your preference."),
                points
        );
    }

    public ReviewSummaryResponse summarizeReviews(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        if (reviews.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No reviews yet for this product");
        }

        String reviewText = reviews.stream()
                .map(r -> "rating=" + r.getRating() + " | " + r.getComment())
                .collect(Collectors.joining("\n"));

        String system = """
                Summarize product reviews into clear likes and complaints.
                Reply with JSON only:
                {"likes":["..."],"complaints":["..."],"overall":"one short sentence"}
                """;

        JsonNode root = parseJson(grokClient.chat(
                system,
                "Product: " + product.getName() + "\nReviews:\n" + reviewText
        ));

        List<String> likes = readStringList(root.path("likes"));
        List<String> complaints = readStringList(root.path("complaints"));

        return new ReviewSummaryResponse(
                product.getId(),
                product.getName(),
                reviews.size(),
                likes,
                complaints,
                root.path("overall").asText("Summary generated from customer reviews.")
        );
    }

    public AdminInsightsResponse adminInsights() {
        List<Order> orders = orderRepository.findAll();
        List<Product> products = productRepository.findAll();

        BigDecimal revenue = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byStatus = Arrays.stream(OrderStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        s -> orders.stream().filter(o -> o.getStatus() == s).count(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PLACED
                        || o.getStatus() == OrderStatus.PACKED
                        || o.getStatus() == OrderStatus.SHIPPED)
                .count();

        Map<String, Long> soldQty = new HashMap<>();
        Map<String, BigDecimal> categorySales = new LinkedHashMap<>();
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                soldQty.merge(item.getProductName(), (long) item.getQuantity(), Long::sum);
                String category = item.getProduct() != null && item.getProduct().getCategory() != null
                        ? item.getProduct().getCategory().getName()
                        : "Other";
                BigDecimal line = item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                categorySales.merge(category, line, BigDecimal::add);
            }
        }

        List<NamedCountDto> topProducts = soldQty.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> new NamedCountDto(e.getKey(), e.getValue()))
                .toList();

        List<CategorySalesDto> salesByCategory = categorySales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> new CategorySalesDto(e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        List<NamedCountDto> lowStockItems = products.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= 10)
                .sorted(Comparator.comparing(Product::getStock))
                .limit(8)
                .map(p -> new NamedCountDto(p.getName(), p.getStock()))
                .toList();

        long lowStock = lowStockItems.size();

        String statsBlock = """
                totalOrders=%d
                totalRevenueINR=%.2f
                pendingOrders=%d
                lowStockCount=%d
                ordersByStatus=%s
                salesByCategory=%s
                topProducts=%s
                lowStockItems=%s
                """.formatted(
                orders.size(),
                revenue,
                pendingOrders,
                lowStock,
                byStatus,
                salesByCategory,
                topProducts,
                lowStockItems
        );

        String system = """
                You are writing a short plain-language store insights summary for an e-commerce admin.
                Use only the provided stats. 3-5 sentences. No markdown.
                Mention restock priorities and pending orders when relevant.
                """;

        String aiSummary = grokClient.chat(system, "Stats:\n" + statsBlock);

        return new AdminInsightsResponse(
                orders.size(),
                revenue.setScale(2, RoundingMode.HALF_UP),
                products.size(),
                lowStock,
                pendingOrders,
                byStatus,
                salesByCategory,
                topProducts,
                lowStockItems,
                aiSummary.trim()
        );
    }

    private void mergeIntoCart(User user, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        CartItem existing = cartItemRepository
                .findByUserIdAndProductId(user.getId(), productId)
                .orElse(null);

        int newQty = quantity + (existing != null ? existing.getQuantity() : 0);
        if (newQty > product.getStock()) {
            newQty = product.getStock();
        }
        if (newQty <= 0) {
            return;
        }

        if (existing != null) {
            existing.setQuantity(newQty);
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        }
    }

    private JsonNode parseJson(String raw) {
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "")
                        .replaceAll("\\s*```$", "")
                        .trim();
            }
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not parse AI response as JSON"
            );
        }
    }

    private static List<String> readStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> {
                if (!n.asText().isBlank()) {
                    list.add(n.asText());
                }
            });
        }
        return list;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String abbreviate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }
}
