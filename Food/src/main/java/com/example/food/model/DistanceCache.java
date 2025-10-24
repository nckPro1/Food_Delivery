package com.example.food.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "distance_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cache_id")
    private Long cacheId;

    @Column(name = "origin_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal originLat;

    @Column(name = "origin_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal originLng;

    @Column(name = "destination_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal destinationLat;

    @Column(name = "destination_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal destinationLng;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "cached_at")
    private LocalDateTime cachedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Helper methods
    public BigDecimal getDistanceKm() {
        return BigDecimal.valueOf(distanceMeters / 1000.0);
    }

    public Integer getDurationMinutes() {
        return durationSeconds / 60;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
