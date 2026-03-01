package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN','DELIVERY_PARTNER')")
    public ResponseEntity<?> me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(authService.profile(user));
    }

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('USER','ADMIN','DELIVERY_PARTNER')")
    public ResponseEntity<?> uploadImage(
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(authService.uploadProfileImage(user, file));
    }
}
