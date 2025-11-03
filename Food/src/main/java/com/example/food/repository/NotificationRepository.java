package com.example.food.repository;

import com.example.food.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy tất cả notifications của user, sắp xếp theo thời gian mới nhất
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Đếm số notifications chưa đọc của user
     */
    Long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Lấy notifications chưa đọc của user
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Đánh dấu tất cả notifications của user là đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * Đánh dấu một notification là đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId);

    /**
     * Xóa notifications cũ hơn một số ngày (để cleanup)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :beforeDate AND n.isRead = true")
    void deleteOldReadNotifications(@Param("beforeDate") java.time.LocalDateTime beforeDate);
}
