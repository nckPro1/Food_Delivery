package com.example.food.repository;

import com.example.food.model.DistanceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DistanceCacheRepository extends JpaRepository<DistanceCache, Long> {

    @Query("SELECT dc FROM DistanceCache dc WHERE " +
           "dc.originLat = :originLat AND dc.originLng = :originLng AND " +
           "dc.destinationLat = :destLat AND dc.destinationLng = :destLng AND " +
           "dc.expiresAt > :now")
    Optional<DistanceCache> findByCoordinates(
            @Param("originLat") BigDecimal originLat,
            @Param("originLng") BigDecimal originLng,
            @Param("destLat") BigDecimal destLat,
            @Param("destLng") BigDecimal destLng,
            @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM DistanceCache dc WHERE dc.expiresAt < :now")
    void deleteExpiredCache(@Param("now") LocalDateTime now);
}
