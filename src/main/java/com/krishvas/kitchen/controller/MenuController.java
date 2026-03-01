package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.MenuRequest;
import com.krishvas.kitchen.entity.Menu;
import com.krishvas.kitchen.entity.Role;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.service.BannerService;
import com.krishvas.kitchen.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/menus")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final BannerService bannerService;

    @GetMapping("/daily")
    public ResponseEntity<?> daily() {
        return ResponseEntity.ok(menuService.dailyMenu());
    }

    @GetMapping("/scheduled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> scheduled(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(menuService.listScheduled(start, end));
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> suggestions() {
        return ResponseEntity.ok(menuService.suggestions());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody MenuRequest request, Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only admin can create menu");
        }
        Menu menu = menuService.create(request, admin);
        return ResponseEntity.ok(menu);
    }

    @PutMapping("/{menuId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long menuId, @RequestBody MenuRequest request, Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.update(menuId, request, admin));
    }

    @DeleteMapping("/{menuId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/banners")
    public ResponseEntity<?> banners() {
        return ResponseEntity.ok(bannerService.activeBanners());
    }
}
