package com.example.food.repository;

import com.example.food.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Tìm sản phẩm theo category
    List<Product> findByCategoryCategoryIdAndIsAvailableTrue(Long categoryId);

    // Tìm sản phẩm featured
    List<Product> findByIsFeaturedTrueAndIsAvailableTrue();

    // Tìm sản phẩm available với pagination
    Page<Product> findByIsAvailableTrue(Pageable pageable);

    // Tìm sản phẩm inactive với pagination
    Page<Product> findByIsAvailableFalse(Pageable pageable);

    // Tìm sản phẩm theo category với pagination
    Page<Product> findByCategoryCategoryIdAndIsAvailableTrue(Long categoryId, Pageable pageable);

    // Tìm sản phẩm inactive theo category với pagination
    Page<Product> findByCategoryCategoryIdAndIsAvailableFalse(Long categoryId, Pageable pageable);

    // Tìm kiếm sản phẩm theo tên
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "p.isAvailable = true")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "p.isAvailable = false")
    Page<Product> searchInactiveByName(@Param("keyword") String keyword, Pageable pageable);

    // Tìm kiếm sản phẩm theo tên và category
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "p.category.categoryId = :categoryId AND " +
            "p.isAvailable = true")
    Page<Product> searchByNameAndCategory(@Param("keyword") String keyword,
                                          @Param("categoryId") Long categoryId,
                                          Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "p.category.categoryId = :categoryId AND " +
            "p.isAvailable = false")
    Page<Product> searchInactiveByNameAndCategory(@Param("keyword") String keyword,
                                                  @Param("categoryId") Long categoryId,
                                                  Pageable pageable);

    // Đếm số sản phẩm theo category
    long countByCategoryCategoryIdAndIsAvailableTrue(Long categoryId);

    // Tìm sản phẩm có options
    List<Product> findByHasOptionsTrueAndIsAvailableTrue();

    // Tìm sản phẩm theo khoảng giá
    @Query("SELECT p FROM Product p WHERE " +
            "p.price BETWEEN :minPrice AND :maxPrice AND " +
            "p.isAvailable = true")
    Page<Product> findByPriceRange(@Param("minPrice") Double minPrice,
                                   @Param("maxPrice") Double maxPrice,
                                   Pageable pageable);

    // Tìm sản phẩm theo thời gian chuẩn bị
    List<Product> findByPreparationTimeLessThanEqualAndIsAvailableTrue(Integer maxPreparationTime);

    // Tìm sản phẩm có ảnh
    List<Product> findByImageUrlIsNotNullAndIsAvailableTrue();

    // Tìm sản phẩm có gallery
    @Query("SELECT p FROM Product p WHERE " +
            "p.galleryUrls IS NOT NULL AND " +
            "p.galleryUrls != '' AND " +
            "p.isAvailable = true")
    List<Product> findProductsWithGallery();
}
