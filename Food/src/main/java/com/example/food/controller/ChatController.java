package com.example.food.controller;

import com.example.food.dto.*;
import com.example.food.model.User;
import com.example.food.repository.UserRepository;
import com.example.food.security.JwtTokenProvider;
import com.example.food.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Lấy user ID từ JWT token
     */
    private Long getUserIdFromToken(String token) {
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUserId();
    }

    // ===============================
    // USER ENDPOINTS
    // ===============================

    /**
     * Tạo conversation mới
     */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationDTO>> createConversation(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateConversationRequest request) {
        try {
            Long userId = getUserIdFromToken(token);
            ConversationDTO conversation = chatService.createConversation(userId, request);
            return ResponseEntity.ok(ApiResponse.<ConversationDTO>builder()
                    .success(true)
                    .message("Conversation created successfully")
                    .data(conversation)
                    .build());
        } catch (Exception e) {
            log.error("Error creating conversation", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ConversationDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy danh sách conversations của user
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationDTO>>> getUserConversations(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            log.debug("Getting conversations for userId: {}", userId);
            List<ConversationDTO> conversations = chatService.getUserConversations(userId);
            log.debug("Found {} conversations for userId: {}", conversations != null ? conversations.size() : 0, userId);
            return ResponseEntity.ok(ApiResponse.<List<ConversationDTO>>builder()
                    .success(true)
                    .message("Conversations retrieved")
                    .data(conversations)
                    .build());
        } catch (Exception e) {
            log.error("Error getting conversations", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<ConversationDTO>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy chi tiết conversation
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationDTO>> getConversation(
            @RequestHeader("Authorization") String token,
            @PathVariable Long conversationId) {
        try {
            Long userId = getUserIdFromToken(token);
            ConversationDTO conversation = chatService.getConversation(conversationId, userId);

            // Đánh dấu messages đã đọc khi xem conversation
            chatService.markMessagesAsRead(conversationId, userId);

            return ResponseEntity.ok(ApiResponse.<ConversationDTO>builder()
                    .success(true)
                    .message("Conversation retrieved")
                    .data(conversation)
                    .build());
        } catch (Exception e) {
            log.error("Error getting conversation", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ConversationDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy messages của conversation
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long conversationId) {
        try {
            Long userId = getUserIdFromToken(token);
            log.debug("Getting messages for conversationId: {}, userId: {}", conversationId, userId);
            List<MessageDTO> messages = chatService.getMessages(conversationId, userId);
            log.debug("Found {} messages for conversationId: {}", messages != null ? messages.size() : 0, conversationId);
            return ResponseEntity.ok(ApiResponse.<List<MessageDTO>>builder()
                    .success(true)
                    .message("Messages retrieved")
                    .data(messages)
                    .build());
        } catch (Exception e) {
            log.error("Error getting messages for conversationId: {}", conversationId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<MessageDTO>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Gửi message
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageDTO>> sendMessage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {
        try {
            Long userId = getUserIdFromToken(token);
            MessageDTO message = chatService.sendMessage(conversationId, userId, request);
            return ResponseEntity.ok(ApiResponse.<MessageDTO>builder()
                    .success(true)
                    .message("Message sent")
                    .data(message)
                    .build());
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.badRequest().body(ApiResponse.<MessageDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Đánh dấu messages đã đọc
     */
    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long conversationId) {
        try {
            Long userId = getUserIdFromToken(token);
            chatService.markMessagesAsRead(conversationId, userId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Messages marked as read")
                    .build());
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // ===============================
    // ADMIN ENDPOINTS
    // ===============================

    /**
     * Admin lấy tất cả conversations
     */
    @GetMapping("/admin/conversations")
    public ResponseEntity<ApiResponse<List<ConversationDTO>>> getAllConversations(
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = getUserIdFromToken(token);
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!admin.isAdmin()) {
                return ResponseEntity.status(403).body(ApiResponse.<List<ConversationDTO>>builder()
                        .success(false)
                        .message("Access denied")
                        .build());
            }

            List<ConversationDTO> conversations = chatService.getAllConversationsForAdmin(adminId);
            return ResponseEntity.ok(ApiResponse.<List<ConversationDTO>>builder()
                    .success(true)
                    .message("All conversations retrieved")
                    .data(conversations)
                    .build());
        } catch (Exception e) {
            log.error("Error getting all conversations", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<ConversationDTO>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}

