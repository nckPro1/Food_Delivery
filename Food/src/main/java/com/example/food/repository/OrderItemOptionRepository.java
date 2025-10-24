package com.example.food.repository;

import com.example.food.model.OrderItemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemOptionRepository extends JpaRepository<OrderItemOption, Long> {

    List<OrderItemOption> findByOrderItemOrderItemId(Long orderItemId);
}
