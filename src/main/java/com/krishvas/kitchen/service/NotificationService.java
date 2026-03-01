package com.krishvas.kitchen.service;

import com.krishvas.kitchen.entity.Notification;
import com.krishvas.kitchen.entity.NotificationType;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Notification createForUser(User user, NotificationType type, String title, String message, String metadataJson) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setMetadataJson(metadataJson);
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        messagingTemplate.convertAndSend("/topic/notifications/user/" + user.getId(), toPayload(saved));
        return saved;
    }

    public void publishRoleEvent(String roleTopic, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/notifications/" + roleTopic, payload);
    }

    public void publishOrderEvent(String orderId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, payload);
    }

    public List<Notification> list(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public Notification markRead(User user, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (n.getUser() == null || !n.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized notification access");
        }
        n.setRead(true);
        return notificationRepository.save(n);
    }

    private Map<String, Object> toPayload(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("type", n.getType());
        map.put("title", n.getTitle());
        map.put("message", n.getMessage());
        map.put("metadataJson", n.getMetadataJson());
        map.put("isRead", n.isRead());
        map.put("createdAt", n.getCreatedAt());
        return map;
    }
}
