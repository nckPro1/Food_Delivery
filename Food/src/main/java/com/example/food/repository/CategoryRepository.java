package com.example.food.repository;

import com.example.food.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tìm category active
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();

    // Tìm category theo tên
    Optional<Category> findByCategoryNameIgnoreCase(String categoryName);

    // Tìm category có sản phẩm
    @Query("SELECT DISTINCT c FROM Category c " +
            "JOIN c.products p " +
            "WHERE c.isActive = true AND p.isAvailable = true " +
            "ORDER BY c.sortOrder ASC")
    List<Category> findCategoriesWithProducts();

    // Đếm số sản phẩm trong category
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId AND p.isAvailable = true")
    long countProductsInCategory(@Param("categoryId") Long categoryId);
}
