package com.ekart.ekart.controller;

import com.ekart.ekart.dto.LoginRequest;
import com.ekart.ekart.dto.RegisterRequest;
import com.ekart.ekart.dto.AuthResponse;
import com.ekart.ekart.model.Role;
import com.ekart.ekart.model.User;
import com.ekart.ekart.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest()
                    .body("Email already registered");
        }

        User user = new User();

        user.setName(request.name());
        user.setEmail(request.email());

        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setRole(Role.CUSTOMER);

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(
                new AuthResponse(
                        savedUser.getId(),
                        savedUser.getName(),
                        savedUser.getEmail(),
                        savedUser.getRole(),
                        "Registration successful"
                )
        );
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

        return ResponseEntity.ok(
                new AuthResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        "Login successful"
                )
        );
    }
}