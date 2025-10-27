package com.example.food.repository;

import com.example.food.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCouponCodeAndIsActiveTrue(String couponCode);

    @Query("SELECT c FROM Coupon c WHERE c.couponCode = :couponCode AND c.isActive = true " +
           "AND c.startDate <= :now AND c.endDate >= :now " +
           "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    Optional<Coupon> findValidCoupon(@Param("couponCode") String couponCode, @Param("now") LocalDateTime now);

    boolean existsByCouponCode(String couponCode);
}


