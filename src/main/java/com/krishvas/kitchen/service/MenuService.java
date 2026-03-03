package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.MenuItemPayload;
import com.krishvas.kitchen.dto.MenuRequest;
import com.krishvas.kitchen.entity.Menu;
import com.krishvas.kitchen.entity.MenuItem;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional
    public Menu create(MenuRequest request, User admin) {
        validateScheduleDate(request.scheduleDate());
        Menu menu = new Menu();
        apply(menu, request, admin);
        return menuRepository.save(menu);
    }

    @Transactional
    public Menu update(Long menuId, MenuRequest request, User admin) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new IllegalArgumentException("Menu not found"));
        validateScheduleDate(request.scheduleDate());
        menu.getItems().clear();
        apply(menu, request, admin);
        return menuRepository.save(menu);
    }

    public void delete(Long menuId) {
        menuRepository.deleteById(menuId);
    }

    public Menu dailyMenu() {
        return menuForDate(LocalDate.now());
    }

    public Menu menuForDate(LocalDate date) {
        if (date.isEqual(LocalDate.now())) {
            return menuRepository.findByScheduleDate(date)
                .or(() -> menuRepository.findByScheduleDateLessThanEqualOrderByScheduleDateDesc(date).stream().findFirst())
                .orElseGet(() -> {
                    Menu empty = new Menu();
                    empty.setTitle("Today's Menu");
                    empty.setDescription("No menu scheduled yet.");
                    empty.setScheduleDate(date);
                    empty.setTemplate(false);
                    empty.setMealSlots("ALL");
                    return empty;
                });
        }
        return menuRepository.findByScheduleDate(date).orElseGet(() -> {
            Menu empty = new Menu();
            empty.setTitle("Menu");
            empty.setDescription("No menu scheduled for selected date.");
            empty.setScheduleDate(date);
            empty.setTemplate(false);
            empty.setMealSlots("ALL");
            return empty;
        });
    }

    public List<Menu> listScheduled(LocalDate start, LocalDate end) {
        return menuRepository.findByScheduleDateBetweenOrderByScheduleDateAsc(start, end);
    }

    public List<Menu> next7DaysMenus() {
        LocalDate start = LocalDate.now();
        return menuRepository.findByScheduleDateBetweenOrderByScheduleDateAsc(start, start.plusDays(6));
    }

    public List<Menu> suggestions() {
        return menuRepository.findTop15ByOrderByCreatedAtDesc();
    }

    private void apply(Menu menu, MenuRequest request, User admin) {
        menu.setTitle(request.title());
        menu.setDescription(request.description());
        menu.setScheduleDate(request.scheduleDate());
        menu.setTemplate(request.template());
        menu.setMealSlots(toMealSlotCsv(request.mealSlots()));
        menu.setCreatedBy(admin);
        for (MenuItemPayload payload : request.items()) {
            MenuItem item = new MenuItem();
            item.setMenu(menu);
            item.setName(payload.name());
            item.setDescription(payload.description());
            item.setPrice(payload.price());
            item.setCategory(payload.category());
            item.setImageUrl(payload.imageUrl());
            item.setAvailable(payload.available());
            menu.getItems().add(item);
        }
    }

    private void validateScheduleDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Cannot schedule menu in the past");
        }
        if (date.isAfter(today.plusDays(30))) {
            throw new IllegalArgumentException("Menu can only be scheduled up to 30 days ahead");
        }
    }

    private String toMealSlotCsv(List<String> slots) {
        List<String> normalized = new ArrayList<>();
        for (String slot : slots == null ? List.<String>of() : slots) {
            String s = slot == null ? "" : slot.trim().toUpperCase(Locale.ROOT);
            if (s.isBlank()) continue;
            if (List.of("ALL", "BREAKFAST", "LUNCH", "DINNER").contains(s)) {
                normalized.add(s);
            }
        }
        if (normalized.isEmpty()) {
            normalized.add("ALL");
        }
        if (normalized.contains("ALL")) {
            return "ALL";
        }
        return normalized.stream().distinct().collect(Collectors.joining(","));
    }
}
