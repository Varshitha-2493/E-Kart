package com.ekart.ekart.config;

import com.ekart.ekart.model.*;
import com.ekart.ekart.repository.CategoryRepository;
import com.ekart.ekart.repository.ProductRepository;
import com.ekart.ekart.repository.ReviewRepository;
import com.ekart.ekart.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      ReviewRepository reviewRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User admin = seedAdmin();
        User demo = seedDemoCustomer();
        if (productRepository.count() > 0) {
            return;
        }
        seedCatalog(admin, demo);
    }

    private User seedAdmin() {
        return userRepository.findByEmail("admin@ekart.com").orElseGet(() -> {
            User user = new User();
            user.setName("E-Kart Admin");
            user.setEmail("admin@ekart.com");
            user.setPasswordHash(passwordEncoder.encode("admin123"));
            user.setRole(Role.ADMIN);
            return userRepository.save(user);
        });
    }

    private User seedDemoCustomer() {
        return userRepository.findByEmail("customer@ekart.com").orElseGet(() -> {
            User user = new User();
            user.setName("Demo Customer");
            user.setEmail("customer@ekart.com");
            user.setPasswordHash(passwordEncoder.encode("customer123"));
            user.setRole(Role.CUSTOMER);
            return userRepository.save(user);
        });
    }

    private void seedCatalog(User admin, User demo) {
        Category electronics = category("Electronics");
        Category party = category("Party & Celebration");
        Category home = category("Home Essentials");
        Category snacks = category("Snacks & Treats");
        Category mobiles = category("Mobiles");

        Product p1 = product("Balloon Pack (50 pcs)", party,
                "Assorted colourful balloons for kids parties.",
                "Count: 50 | Material: Latex | Ages: 3+",
                "299.00", 80,
                "https://images.unsplash.com/photo-1530103862676-de8c9debad1d?w=600", 4.5);

        Product p2 = product("Party Caps Set (10)", party,
                "Cone party caps for birthday celebrations.",
                "Pack of 10 | Adjustable | Paper",
                "199.00", 60,
                "https://images.unsplash.com/photo-1464349095431-e9a21285b5f3?w=600", 4.2);

        Product p3 = product("Return Gift Combo", party,
                "Small toys and stationery suitable as return gifts.",
                "10 gift packs | Mixed toys | Ages 5-12",
                "899.00", 40,
                "https://images.unsplash.com/photo-1513201099705-a9746e1e201f?w=600", 4.4);

        Product p4 = product("Chocolate Assortment Box", snacks,
                "Assorted chocolates for celebrations and gifting.",
                "500g | Mixed flavours | Vegetarian",
                "549.00", 55,
                "https://images.unsplash.com/photo-1549007994-cb92caebd54b?w=600", 4.6);

        Product p5 = product("Paper Cups & Plates Kit", party,
                "Disposable cups and plates for 12 guests.",
                "12 plates + 12 cups | Eco paper",
                "249.00", 70,
                "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=600", 4.1);

        Product p6 = product("Streamers & Banners Kit", party,
                "Happy Birthday banner with colourful streamers.",
                "1 banner + 6 streamers | Reusable",
                "349.00", 45,
                "https://images.unsplash.com/photo-1513151233558-d860c5398176?w=600", 4.3);

        Product p7 = product("NovaPhone X1", mobiles,
                "Budget smartphone with solid everyday performance.",
                "Battery: 5000mAh | Camera: 48MP | RAM: 6GB | Storage: 128GB",
                "12999.00", 25,
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600", 4.0);

        Product p8 = product("PulsePhone Z5", mobiles,
                "Camera-focused phone with fast charging.",
                "Battery: 4500mAh | Camera: 64MP | RAM: 8GB | Storage: 256GB",
                "18999.00", 18,
                "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600", 4.5);

        Product p9 = product("AeroPhone Lite", mobiles,
                "Lightweight phone with long battery life.",
                "Battery: 6000mAh | Camera: 32MP | RAM: 4GB | Storage: 128GB",
                "9999.00", 30,
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600", 4.2);

        Product p10 = product("LED Desk Lamp", home,
                "Adjustable LED lamp for study and work.",
                "3 brightness modes | USB powered | Touch control",
                "799.00", 35,
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600", 4.4);

        Product p11 = product("Wireless Earbuds Pro", electronics,
                "Bluetooth earbuds with clear calls and deep bass.",
                "Battery: 24h case | Bluetooth 5.3 | IPX4",
                "2499.00", 50,
                "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600", 4.3);

        Product p12 = product("Portable Bluetooth Speaker", electronics,
                "Compact speaker for indoor and outdoor use.",
                "Battery: 12h | Waterproof | Dual pairing",
                "1799.00", 28,
                "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600", 4.1);

        List.of(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)
                .forEach(productRepository::save);

        seedReviews(demo, p7, p8, p9, p11, p1);
    }

    private void seedReviews(User user, Product... products) {
        addReview(products[0], user, 4, "Good value phone. Battery lasts a full day of normal use.");
        addReview(products[0], user, 3, "Camera is average in low light. Fine for the price.");
        addReview(products[1], user, 5, "Excellent camera and smooth performance. Worth the extra money.");
        addReview(products[1], user, 4, "Charges fast and photos look sharp. A bit bulky.");
        addReview(products[2], user, 5, "Battery life is outstanding. Perfect if you hate charging often.");
        addReview(products[2], user, 3, "Camera quality is weaker than pricier phones.");
        addReview(products[3], user, 5, "Great sound and comfortable fit for workouts.");
        addReview(products[3], user, 4, "Good bass. Case charging could be faster.");
        addReview(products[4], user, 5, "Kids loved the colours. Good quantity for a small party.");
        addReview(products[4], user, 4, "Nice pack, a few balloons were thin.");
    }

    private void addReview(Product product, User user, int rating, String comment) {
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);

        List<Review> all = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(rating);
        product.setAverageRating(Math.round(avg * 10.0) / 10.0);
        productRepository.save(product);
    }

    private Category category(String name) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(name);
                    return categoryRepository.save(c);
                });
    }

    private Product product(String name, Category category, String description, String specs,
                            String price, int stock, String imageUrl, double rating) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setDescription(description);
        p.setSpecs(specs);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setImageUrl(imageUrl);
        p.setAverageRating(rating);
        return p;
    }
}
