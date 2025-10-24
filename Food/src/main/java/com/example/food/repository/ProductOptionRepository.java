package com.example.food.repository;

import com.example.food.model.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    // Tìm tất cả options của một product
    List<ProductOption> findByProductIdOrderByOptionTypeAscOptionNameAsc(Long productId);

    // Tìm options theo type
    List<ProductOption> findByProductIdAndOptionTypeOrderByOptionNameAsc(Long productId, ProductOption.OptionType optionType);

    // Tìm required options
    List<ProductOption> findByProductIdAndIsRequiredTrueOrderByOptionTypeAscOptionNameAsc(Long productId);

    // Xóa tất cả options của một product
    void deleteByProductId(Long productId);

    // Đếm số options của một product
    long countByProductId(Long productId);

    // Tìm options có price > 0
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.price > 0 ORDER BY po.optionType, po.optionName")
    List<ProductOption> findPaidOptionsByProductId(@Param("productId") Long productId);
}
