package com.example.food.repository;

import com.example.food.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    List<CouponUsage> findByCouponId(Long couponId);

    List<CouponUsage> findByUserId(Long userId);

    List<CouponUsage> findByOrderId(Long orderId);

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.couponId = :couponId AND cu.userId = :userId")
    List<CouponUsage> findByCouponIdAndUserId(@Param("couponId") Long couponId, @Param("userId") Long userId);

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);
}
