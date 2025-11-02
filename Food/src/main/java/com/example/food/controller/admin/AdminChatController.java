package com.example.food.controller.admin;

import com.example.food.dto.*;
import com.example.food.model.User;
import com.example.food.repository.UserRepository;
import com.example.food.security.JwtTokenProvider;
import com.example.food.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Controller
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
@Slf4j
public class AdminChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Lấy user ID từ session hoặc SecurityContext
     */
    private Long getAdminIdFromSession(Model model, HttpServletRequest request) {
        // 1. Thử lấy từ SecurityContext (Authentication)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            log.debug("Authentication principal type: {}, value: {}", principal.getClass().getSimpleName(), principal);

            // Nếu principal là UserDetails, lấy email
            if (principal instanceof UserDetails) {
                String email = ((UserDetails) principal).getUsername();
                log.debug("Extracted email from UserDetails: {}", email);
                try {
                    User user = userRepository.findByEmail(email)
                            .orElse(null);
                    if (user != null) {
                        log.debug("User found: userId={}, email={}, roleId={}, isAdmin={}",
                                user.getUserId(), user.getEmail(), user.getRoleId(), user.isAdmin());
                        if (user.isAdmin()) {
                            log.info("Found admin ID from SecurityContext (UserDetails): {}", user.getUserId());
                            return user.getUserId();
                        } else {
                            log.warn("User {} is not admin (roleId={})", email, user.getRoleId());
                        }
                    } else {
                        log.warn("User not found in database for email: {}", email);
                    }
                } catch (Exception e) {
                    log.error("Error getting user from email: {}", email, e);
                }
            }

            // Nếu principal là String (email)
            if (principal instanceof String) {
                String email = (String) principal;
                log.debug("Extracted email from String principal: {}", email);
                try {
                    User user = userRepository.findByEmail(email)
                            .orElse(null);
                    if (user != null) {
                        log.debug("User found: userId={}, email={}, roleId={}, isAdmin={}",
                                user.getUserId(), user.getEmail(), user.getRoleId(), user.isAdmin());
                        if (user.isAdmin()) {
                            log.info("Found admin ID from SecurityContext (String): {}", user.getUserId());
                            return user.getUserId();
                        } else {
                            log.warn("User {} is not admin (roleId={})", email, user.getRoleId());
                        }
                    } else {
                        log.warn("User not found in database for email: {}", email);
                    }
                } catch (Exception e) {
                    log.error("Error getting user from email: {}", email, e);
                }
            }
        } else {
            log.debug("Authentication is null or not authenticated");
        }

        // 2. Thử lấy từ session
        Object userId = request.getSession().getAttribute("adminUserId");
        if (userId instanceof Long) {
            log.debug("Found admin ID from session: {}", userId);
            return (Long) userId;
        }

        // 3. Thử lấy từ model (nếu có interceptor set)
        Object modelUserId = model.getAttribute("adminUserId");
        if (modelUserId instanceof Long) {
            log.debug("Found admin ID from model: {}", modelUserId);
            return (Long) modelUserId;
        }

        // 4. Fallback: lấy admin đầu tiên (tạm thời cho demo)
        // Trong production, nên throw exception nếu không tìm thấy
        log.warn("Could not find admin ID, using fallback - getting first admin");
        return userRepository.findByRoleIdAndIsActiveTrue(1) // Admin role = 1 (theo database)
                .stream()
                .findFirst()
                .map(User::getUserId)
                .orElse(1L);
    }

    /**
     * Trang danh sách conversations cho admin
     */
    @GetMapping
    public String chatList(Model model, HttpServletRequest request) {
        try {
            Long adminId = getAdminIdFromSession(model, request);

            // Kiểm tra admin
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            if (!admin.isAdmin()) {
                return "redirect:/admin/login?error=access_denied";
            }

            List<ConversationDTO> conversations = chatService.getAllConversationsForAdmin(adminId);
            model.addAttribute("conversations", conversations);
            model.addAttribute("pageTitle", "Quản lý Chat");
            return "admin/chat/list";
        } catch (Exception e) {
            log.error("Error loading conversations", e);
            model.addAttribute("error", "Lỗi khi tải danh sách chat: " + e.getMessage());
            return "admin/chat/list";
        }
    }

    /**
     * Trang chat chi tiết với user
     */
    @GetMapping("/{conversationId}")
    public String chatDetail(@PathVariable Long conversationId, Model model, HttpServletRequest request) {
        try {
            Long adminId = getAdminIdFromSession(model, request);

            // Kiểm tra admin
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            if (!admin.isAdmin()) {
                return "redirect:/admin/login?error=access_denied";
            }

            ConversationDTO conversation = chatService.getConversation(conversationId, adminId);
            List<MessageDTO> messages = chatService.getMessages(conversationId, adminId);

            // Đánh dấu đã đọc
            chatService.markMessagesAsRead(conversationId, adminId);

            model.addAttribute("conversation", conversation);
            model.addAttribute("messages", messages);
            model.addAttribute("conversationId", conversationId);
            model.addAttribute("adminId", adminId);
            model.addAttribute("pageTitle", "Chat với " + conversation.getCreatedByName());
            return "admin/chat/detail";
        } catch (Exception e) {
            log.error("Error loading conversation", e);
            model.addAttribute("error", "Lỗi khi tải cuộc trò chuyện: " + e.getMessage());
            return "redirect:/admin/chat";
        }
    }

    /**
     * API endpoint để gửi message (AJAX)
     */
    @PostMapping("/{conversationId}/send")
    @ResponseBody
    public ResponseEntity<ApiResponse<MessageDTO>> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            Model model,
            HttpServletRequest httpRequest) {
        try {
            log.info("========================================");
            log.info("=== Admin Send Message Request START ===");
            log.info("ConversationId: {}", conversationId);
            log.info("Request object: {}", request);
            log.info("Request method: {}", httpRequest.getMethod());
            log.info("Request URI: {}", httpRequest.getRequestURI());
            log.info("Request headers - Content-Type: {}", httpRequest.getHeader("Content-Type"));
            log.info("Request headers - Accept: {}", httpRequest.getHeader("Accept"));

            if (request == null) {
                log.error("Request body is null");
                return ResponseEntity.badRequest().body(ApiResponse.<MessageDTO>builder()
                        .success(false)
                        .message("Request body is required")
                        .build());
            }

            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                log.error("Message content is empty");
                return ResponseEntity.badRequest().body(ApiResponse.<MessageDTO>builder()
                        .success(false)
                        .message("Message content is required")
                        .build());
            }

            Long adminId = getAdminIdFromSession(model, httpRequest);
            log.info("AdminId from session: {}", adminId);

            if (adminId == null) {
                log.error("AdminId is null - cannot send message");
                return ResponseEntity.status(401).body(ApiResponse.<MessageDTO>builder()
                        .success(false)
                        .message("Admin not authenticated")
                        .build());
            }

            log.info("Admin {} sending message to conversation {}", adminId, conversationId);
            log.info("Message content: {}", request.getContent());

            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> {
                        log.error("Admin with id {} not found", adminId);
                        return new RuntimeException("Admin not found");
                    });

            if (!admin.isAdmin()) {
                log.warn("Non-admin user {} attempted to send message", adminId);
                return ResponseEntity.status(403).body(ApiResponse.<MessageDTO>builder()
                        .success(false)
                        .message("Access denied")
                        .build());
            }

            MessageDTO message = chatService.sendMessage(conversationId, adminId, request);
            log.info("Message sent successfully: messageId={}, content={}",
                    message.getMessageId(), message.getContent());
            log.info("Message isOwnMessage: {}", message.getIsOwnMessage());

            return ResponseEntity.ok(ApiResponse.<MessageDTO>builder()
                    .success(true)
                    .message("Message sent")
                    .data(message)
                    .build());
        } catch (Exception e) {
            log.error("Error sending message from admin to conversation {}", conversationId, e);
            log.error("Exception details: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<MessageDTO>builder()
                    .success(false)
                    .message(e.getMessage() != null ? e.getMessage() : "Internal server error")
                    .build());
        }
    }

    /**
     * API endpoint để lấy messages (AJAX)
     */
    @GetMapping("/{conversationId}/messages")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getMessages(
            @PathVariable Long conversationId,
            Model model,
            HttpServletRequest httpRequest) {
        try {
            Long adminId = getAdminIdFromSession(model, httpRequest);

            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            if (!admin.isAdmin()) {
                return ResponseEntity.status(403).body(ApiResponse.<List<MessageDTO>>builder()
                        .success(false)
                        .message("Access denied")
                        .build());
            }

            List<MessageDTO> messages = chatService.getMessages(conversationId, adminId);
            return ResponseEntity.ok(ApiResponse.<List<MessageDTO>>builder()
                    .success(true)
                    .message("Messages retrieved")
                    .data(messages)
                    .build());
        } catch (Exception e) {
            log.error("Error getting messages", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<MessageDTO>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}

