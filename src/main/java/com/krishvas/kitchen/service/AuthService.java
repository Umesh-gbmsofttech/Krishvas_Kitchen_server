package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.AuthResponse;
import com.krishvas.kitchen.dto.LoginRequest;
import com.krishvas.kitchen.dto.RegisterRequest;
import com.krishvas.kitchen.dto.UserProfileResponse;
import com.krishvas.kitchen.entity.Admin;
import com.krishvas.kitchen.entity.Role;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.AdminRepository;
import com.krishvas.kitchen.repository.UserRepository;
import com.krishvas.kitchen.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ImageService imageService;
    private final ImageUrlService imageUrlService;

    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("Email already registered");
        });
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        User saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return toAuthResponse(user);
    }

    public AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        String profileImageUrl = imageUrlService.toImageUrl(user.getProfileImageId());
        return new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), token, profileImageUrl);
    }

    public UserProfileResponse profile(User user) {
        User current = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserProfileResponse(
            current.getId(),
            current.getFullName(),
            current.getEmail(),
            current.getPhone(),
            current.getRole(),
            current.isDeliveryBadge(),
            imageUrlService.toImageUrl(current.getProfileImageId())
        );
    }

    public UserProfileResponse uploadProfileImage(User user, MultipartFile file) {
        User current = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        var uploaded = imageService.upload(file, "PROFILE", user.getId());
        current.setProfileImageId(uploaded.getId());
        userRepository.save(current);
        return profile(current);
    }

    @Transactional
    public User ensureAdminSeed(String name, String email, String password) {
        String configuredEmail = email.toLowerCase();

        User configuredAdmin = userRepository.findByEmail(configuredEmail).orElseGet(() -> {
            User user = new User();
            user.setEmail(configuredEmail);
            user.setPhone("9999999999");
            return user;
        });

        configuredAdmin.setFullName(name);
        configuredAdmin.setRole(Role.ADMIN);
        if (configuredAdmin.getPasswordHash() == null || !passwordEncoder.matches(password, configuredAdmin.getPasswordHash())) {
            configuredAdmin.setPasswordHash(passwordEncoder.encode(password));
        }
        User savedConfiguredAdmin = userRepository.save(configuredAdmin);

        // Ensure no other admin user remains in the system.
        List<User> allAdmins = userRepository.findByRole(Role.ADMIN);
        for (User adminUser : allAdmins) {
            if (!adminUser.getId().equals(savedConfiguredAdmin.getId())) {
                adminUser.setRole(Role.USER);
                userRepository.save(adminUser);
            }
        }

        // Keep exactly one row in admin table for configured admin.
        for (Admin adminRecord : adminRepository.findAll()) {
            if (!adminRecord.getUser().getId().equals(savedConfiguredAdmin.getId())) {
                adminRepository.delete(adminRecord);
            }
        }
        if (adminRepository.findByUser(savedConfiguredAdmin).isEmpty()) {
            Admin admin = new Admin();
            admin.setUser(savedConfiguredAdmin);
            adminRepository.save(admin);
        }

        return savedConfiguredAdmin;
    }
}
