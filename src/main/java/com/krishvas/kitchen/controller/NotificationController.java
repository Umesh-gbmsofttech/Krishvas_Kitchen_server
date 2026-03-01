package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.UserRepository;
import com.krishvas.kitchen.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN','DELIVERY_PARTNER')")
    public ResponseEntity<?> list(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.list(user));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('USER','ADMIN','DELIVERY_PARTNER')")
    public ResponseEntity<?> unreadCount(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(user)));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('USER','ADMIN','DELIVERY_PARTNER')")
    public ResponseEntity<?> markRead(@PathVariable Long notificationId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.markRead(user, notificationId));
    }
}
