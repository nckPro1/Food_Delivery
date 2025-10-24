package com.example.food.repository;

import com.example.food.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByPaymentStatus(Payment.PaymentStatus status);
}
