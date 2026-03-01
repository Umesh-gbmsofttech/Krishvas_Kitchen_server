package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.AuthResponse;
import com.krishvas.kitchen.dto.LoginRequest;
import com.krishvas.kitchen.dto.RegisterRequest;
import com.krishvas.kitchen.entity.Admin;
import com.krishvas.kitchen.entity.Role;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.AdminRepository;
import com.krishvas.kitchen.repository.UserRepository;
import com.krishvas.kitchen.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        return new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), token);
    }

    public void ensureAdminSeed(String name, String email, String password) {
        if (userRepository.findByEmail(email.toLowerCase()).isPresent()) {
            return;
        }
        User adminUser = new User();
        adminUser.setFullName(name);
        adminUser.setEmail(email.toLowerCase());
        adminUser.setPhone("9999999999");
        adminUser.setPasswordHash(passwordEncoder.encode(password));
        adminUser.setRole(Role.ADMIN);
        User saved = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUser(saved);
        adminRepository.save(admin);
    }
}
