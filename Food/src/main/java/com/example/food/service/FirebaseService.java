package com.example.food.service;

import com.example.food.dto.MessageDTO;
import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class FirebaseService {

    private final FirebaseDatabase firebaseDatabase;
    private final ExecutorService executorService;

    @Autowired
    public FirebaseService(FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
        // Shared executor để xử lý Firebase async operations
        this.executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * Gửi message lên Firebase Realtime Database sử dụng Firebase Admin SDK
     *
     * Ưu điểm so với REST API:
     * - Bảo mật tốt hơn: Dùng Service Account Key (không thể lộ như token)
     * - Code sạch hơn: Dùng methods Java rõ ràng thay vì tự viết HTTP request
     * - Tách biệt rõ ràng: Server hoạt động như Admin không bị cản trở
     * - Error handling tốt hơn: Có retry và error handling tự động
     */
    public void sendMessageToFirebase(Long conversationId, MessageDTO message) {
        log.info("🔥 FirebaseService.sendMessageToFirebase called: conversationId={}, messageId={}",
                conversationId, message.getMessageId());
        try {
            // Convert LocalDateTime to milliseconds timestamp
            long timestamp;
            if (message.getCreatedAt() != null) {
                ZonedDateTime zonedDateTime = message.getCreatedAt().atZone(java.time.ZoneId.systemDefault());
                timestamp = zonedDateTime.toInstant().toEpochMilli();
            } else {
                timestamp = System.currentTimeMillis();
            }

            // Tạo message data giống như app Android
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", message.getMessageId());
            messageData.put("content", message.getContent());
            messageData.put("senderUserId", message.getSenderUserId());
            messageData.put("senderName", message.getSenderName());
            messageData.put("createdAt", timestamp);
            messageData.put("isOwnMessage", message.getIsOwnMessage());

            log.info("📦 Message data prepared: {}", messageData);

            // Firebase path: /conversations/{conversationId}/messages/{messageId}
            String path = String.format("conversations/%d/messages/%d",
                    conversationId, message.getMessageId());

            log.info("🌐 Firebase path: {}", path);

            // Lấy DatabaseReference và gửi message lên Firebase
            DatabaseReference messageRef = firebaseDatabase.getReference(path);

            log.info("📤 Sending message to Firebase using Admin SDK...");

            // Gửi message async (không block) - Firebase Admin SDK dùng ApiFuture
            ApiFuture<Void> future = messageRef.setValueAsync(messageData);

            // Xử lý kết quả async trong thread riêng để không block
            executorService.submit(() -> {
                try {
                    future.get(); // Get result và check exception nếu có
                    log.info("✅ Message sent to Firebase successfully: conversationId={}, messageId={}, timestamp={}",
                            conversationId, message.getMessageId(), timestamp);
                } catch (Exception e) {
                    log.error("❌ Error sending message to Firebase: conversationId={}, messageId={}",
                            conversationId, message.getMessageId(), e);
                }
            });

            // Note: Không await future.get() ở đây để không block thread chính
            // Firebase Admin SDK sẽ tự động retry nếu có lỗi network

        } catch (Exception e) {
            log.error("❌ Error sending message to Firebase: conversationId={}, messageId={}",
                    conversationId, message.getMessageId(), e);
            // Không throw exception để không làm gián đoạn flow chính
        }
    }
}

