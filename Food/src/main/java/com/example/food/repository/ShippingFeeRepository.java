package com.example.food.repository;

import com.example.food.model.ShippingFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingFeeRepository extends JpaRepository<ShippingFee, Long> {
    
    @Query("SELECT sf FROM ShippingFee sf WHERE sf.isActive = true ORDER BY sf.isDefault DESC, sf.minOrderAmount ASC")
    List<ShippingFee> findAllActiveOrderByDefaultAndMinAmount();
    
    @Query("SELECT sf FROM ShippingFee sf WHERE sf.isActive = true AND sf.isDefault = true")
    Optional<ShippingFee> findDefaultActiveShippingFee();
    
    @Query("SELECT sf FROM ShippingFee sf WHERE sf.isActive = true AND " +
           "(:orderAmount >= sf.minOrderAmount OR sf.minOrderAmount IS NULL) AND " +
           "(:orderAmount <= sf.maxOrderAmount OR sf.maxOrderAmount IS NULL) " +
           "ORDER BY sf.minOrderAmount ASC")
    List<ShippingFee> findApplicableShippingFees(@Param("orderAmount") BigDecimal orderAmount);
    
    @Query("SELECT sf FROM ShippingFee sf WHERE sf.isActive = true AND " +
           "(:orderAmount >= sf.minOrderAmount OR sf.minOrderAmount IS NULL) AND " +
           "(:orderAmount <= sf.maxOrderAmount OR sf.maxOrderAmount IS NULL) " +
           "ORDER BY sf.isDefault DESC, sf.minOrderAmount ASC LIMIT 1")
    Optional<ShippingFee> findBestApplicableShippingFee(@Param("orderAmount") BigDecimal orderAmount);
    
    @Query("SELECT sf FROM ShippingFee sf WHERE sf.isActive = true AND sf.feeType = 'ORDER_BASED' AND " +
           "(:orderAmount >= sf.minOrderAmount OR sf.minOrderAmount IS NULL) AND " +
           "(:orderAmount <= sf.maxOrderAmount OR sf.maxOrderAmount IS NULL) " +
           "ORDER BY sf.minOrderAmount ASC LIMIT 1")
    Optional<ShippingFee> findBestApplicableOrderBasedFee(@Param("orderAmount") BigDecimal orderAmount);
}