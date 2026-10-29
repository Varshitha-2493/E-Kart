package com.ekart.ekart.repository;

import com.ekart.ekart.model.Order;
import com.ekart.ekart.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);
}