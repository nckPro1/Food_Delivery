package com.example.food.service;

import com.example.food.dto.*;
import com.example.food.model.*;
import com.example.food.repository.ConversationRepository;
import com.example.food.repository.ConversationParticipantRepository;
import com.example.food.repository.MessageRepository;
import com.example.food.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private FirebaseService firebaseService;

    /**
     * Tạo conversation mới cho user với admin
     */
    @Transactional
    public ConversationDTO createConversation(Long userId, CreateConversationRequest request) {
        // Tạo conversation
        Conversation conversation = Conversation.builder()
                .subject(request.getSubject())
                .createdByUserId(userId)
                .status(Conversation.ConversationStatus.OPEN)
                .build();
        conversation = conversationRepository.save(conversation);

        // Thêm user làm participant
        ConversationParticipant userParticipant = ConversationParticipant.builder()
                .conversationId(conversation.getConversationId())
                .userId(userId)
                .build();
        participantRepository.save(userParticipant);

        // Thêm tất cả admin làm participants (để admin nào cũng có thể tham gia)
        // Tránh thêm user nếu user đó cũng là admin (để tránh duplicate)
        List<User> admins = userRepository.findByRoleIdAndIsActiveTrue(1); // role_id = 1 là admin (theo database)
        for (User admin : admins) {
            // Chỉ thêm admin nếu không phải là user đã tạo conversation (tránh duplicate)
            if (!admin.getUserId().equals(userId)) {
                ConversationParticipant adminParticipant = ConversationParticipant.builder()
                        .conversationId(conversation.getConversationId())
                        .userId(admin.getUserId())
                        .build();
                participantRepository.save(adminParticipant);
            }
        }

        // Nếu có first message, gửi luôn
        if (request.getFirstMessage() != null && !request.getFirstMessage().trim().isEmpty()) {
            sendMessage(conversation.getConversationId(), userId,
                    SendMessageRequest.builder().content(request.getFirstMessage()).build());
        }

        return convertToConversationDTO(conversation, userId);
    }

    /**
     * Lấy danh sách conversations của user
     */
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        return conversations.stream()
                .map(conv -> convertToConversationDTO(conv, userId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả conversations cho admin (có thể thấy tất cả conversations của users)
     */
    public List<ConversationDTO> getAllConversationsForAdmin(Long adminId) {
        List<Conversation> allConversations = conversationRepository.findAll();
        return allConversations.stream()
                .filter(conv -> !conv.getCreatedByUserId().equals(adminId) || conv.getCreatedByUserId().equals(adminId))
                .map(conv -> convertToConversationDTO(conv, adminId))
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết conversation
     */
    public ConversationDTO getConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Kiểm tra user có quyền truy cập không
        // Admin có quyền truy cập tất cả conversations
        User user = userRepository.findById(userId).orElse(null);
        boolean isAdmin = user != null && user.isAdmin();

        if (!isAdmin) {
            // Chỉ check participant nếu không phải admin
            List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
            boolean hasAccess = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
            if (!hasAccess) {
                throw new RuntimeException("Access denied");
            }
        }

        return convertToConversationDTO(conversation, userId);
    }

    /**
     * Gửi message
     */
    @Transactional
    public MessageDTO sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Kiểm tra user có trong conversation không
        // Admin có quyền gửi message vào bất kỳ conversation nào
        User user = userRepository.findById(senderId).orElse(null);
        boolean isAdmin = user != null && user.isAdmin();

        if (!isAdmin) {
            // Chỉ check participant nếu không phải admin
            List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
            boolean isParticipant = participants.stream().anyMatch(p -> p.getUserId().equals(senderId));
            if (!isParticipant) {
                throw new RuntimeException("You are not a participant of this conversation");
            }
        }

        // Tạo message
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderUserId(senderId)
                .content(request.getContent())
                .attachmentUrls(request.getAttachmentUrls() != null ?
                        String.join(",", request.getAttachmentUrls()) : null)
                .isRead(false)
                .build();
        message = messageRepository.save(message);

        // Cập nhật updated_at của conversation
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        MessageDTO messageDTO = convertToMessageDTO(message, senderId);

        // Gửi message lên Firebase để real-time sync với app Android
        if (firebaseService != null) {
            log.info("🔥 ChatService: FirebaseService is available, sending message to Firebase");
            try {
                firebaseService.sendMessageToFirebase(conversationId, messageDTO);
                log.info("✅ ChatService: FirebaseService.sendMessageToFirebase completed");
            } catch (Exception e) {
                // Log error nhưng không throw để không làm gián đoạn flow
                log.error("❌ ChatService: Error sending message to Firebase: {}", e.getMessage(), e);
            }
        } else {
            log.warn("⚠️ ChatService: FirebaseService is NULL - message will NOT be sent to Firebase!");
        }

        return messageDTO;
    }

    /**
     * Lấy messages của conversation
     */
    public List<MessageDTO> getMessages(Long conversationId, Long userId) {
        // Kiểm tra quyền truy cập
        // Admin có quyền truy cập tất cả conversations
        User user = userRepository.findById(userId).orElse(null);
        boolean isAdmin = user != null && user.isAdmin();

        if (!isAdmin) {
            // Chỉ check participant nếu không phải admin
            List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
            boolean hasAccess = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
            if (!hasAccess) {
                throw new RuntimeException("Access denied");
            }
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream()
                .map(msg -> convertToMessageDTO(msg, userId))
                .collect(Collectors.toList());
    }

    /**
     * Đánh dấu messages đã đọc
     */
    @Transactional
    public void markMessagesAsRead(Long conversationId, Long userId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        for (Message msg : messages) {
            if (!msg.getSenderUserId().equals(userId) && !msg.getIsRead()) {
                msg.setIsRead(true);
                messageRepository.save(msg);
            }
        }
    }

    /**
     * Đếm số unread messages
     */
    public Long getUnreadCount(Long conversationId, Long userId) {
        return messageRepository.countUnreadMessages(conversationId, userId);
    }

    /**
     * Convert Conversation to DTO
     */
    private ConversationDTO convertToConversationDTO(Conversation conversation, Long currentUserId) {
        User creator = userRepository.findById(conversation.getCreatedByUserId())
                .orElse(null);

        // Lấy last message
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getConversationId());
        MessageDTO lastMessage = null;
        if (!messages.isEmpty()) {
            lastMessage = convertToMessageDTO(messages.get(messages.size() - 1), currentUserId);
        }

        // Lấy participants
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getConversationId());
        List<ParticipantDTO> participantDTOs = participants.stream()
                .map(p -> {
                    User user = userRepository.findById(p.getUserId()).orElse(null);
                    if (user != null) {
                        return ParticipantDTO.builder()
                                .userId(user.getUserId())
                                .userName(user.getFullName())
                                .userAvatarUrl(user.getAvatarUrl())
                                .isAdmin(user.isAdmin())
                                .joinedAt(p.getJoinedAt())
                                .build();
                    }
                    return null;
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());

        // Đếm unread
        Long unreadCount = getUnreadCount(conversation.getConversationId(), currentUserId);

        return ConversationDTO.builder()
                .conversationId(conversation.getConversationId())
                .subject(conversation.getSubject())
                .createdByUserId(conversation.getCreatedByUserId())
                .createdByName(creator != null ? creator.getFullName() : null)
                .createdByAvatarUrl(creator != null ? creator.getAvatarUrl() : null)
                .status(conversation.getStatus().name())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessage(lastMessage)
                .participants(participantDTOs)
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * Convert Message to DTO
     */
    private MessageDTO convertToMessageDTO(Message message, Long currentUserId) {
        User sender = userRepository.findById(message.getSenderUserId()).orElse(null);

        List<String> attachmentUrls = new ArrayList<>();
        if (message.getAttachmentUrls() != null && !message.getAttachmentUrls().isEmpty()) {
            attachmentUrls = List.of(message.getAttachmentUrls().split(","));
        }

        return MessageDTO.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversationId())
                .senderUserId(message.getSenderUserId())
                .senderName(sender != null ? sender.getFullName() : null)
                .senderAvatarUrl(sender != null ? sender.getAvatarUrl() : null)
                .content(message.getContent())
                .attachmentUrls(attachmentUrls)
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .isOwnMessage(message.getSenderUserId().equals(currentUserId))
                .build();
    }
}

