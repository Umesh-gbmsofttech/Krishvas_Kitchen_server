package com.krishvas.kitchen.config;

import com.krishvas.kitchen.dto.MenuItemPayload;
import com.krishvas.kitchen.dto.MenuRequest;
import com.krishvas.kitchen.entity.MenuCategory;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.UserRepository;
import com.krishvas.kitchen.service.AuthService;
import com.krishvas.kitchen.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SeedDataConfig {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final MenuService menuService;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner seedDataRunner() {
        return args -> {
            authService.ensureAdminSeed(adminName, adminEmail, adminPassword);
            User admin = userRepository.findByEmail(adminEmail.toLowerCase()).orElseThrow();

            if (menuService.suggestions().isEmpty()) {
                MenuRequest menuToday = new MenuRequest(
                    "Chef's Signature Lunch",
                    "Freshly cooked North Indian meal with rich spices and balanced nutrition.",
                    LocalDate.now(),
                    true,
                    List.of(
                        new MenuItemPayload("Butter Chicken", "Creamy tomato gravy chicken", new BigDecimal("289.00"), MenuCategory.MAINS, "mutton.jpg", true),
                        new MenuItemPayload("Gulab Jamun", "Warm dessert with saffron syrup", new BigDecimal("89.00"), MenuCategory.DESSERTS, "mutton.jpg", true),
                        new MenuItemPayload("Masala Chaas", "Spiced buttermilk", new BigDecimal("49.00"), MenuCategory.BEVERAGES, "mutton.jpg", true)
                    )
                );
                menuService.create(menuToday, admin);

                MenuRequest tomorrow = new MenuRequest(
                    "Weekend Specials",
                    "Special slow-cooked recipes for the weekend crowd.",
                    LocalDate.now().plusDays(1),
                    true,
                    List.of(
                        new MenuItemPayload("Mutton Rogan Josh", "Kashmiri style mutton curry", new BigDecimal("349.00"), MenuCategory.SPECIALS, "mutton.jpg", true),
                        new MenuItemPayload("Firni", "Saffron rice pudding", new BigDecimal("99.00"), MenuCategory.DESSERTS, "mutton.jpg", true)
                    )
                );
                menuService.create(tomorrow, admin);
            }
        };
    }
}
