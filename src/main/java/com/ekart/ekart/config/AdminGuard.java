package com.ekart.ekart.config;

import com.ekart.ekart.exception.ApiException;
import com.ekart.ekart.model.Role;
import com.ekart.ekart.model.User;
import com.ekart.ekart.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AdminGuard {

    private final UserRepository users;

    public AdminGuard(UserRepository users) {
        this.users = users;
    }

    public void require(Long userId) {

        if (userId == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in"
            );
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Unknown user"
                ));

        if (user.getRole() != Role.ADMIN) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Admin only"
            );
        }
    }
}