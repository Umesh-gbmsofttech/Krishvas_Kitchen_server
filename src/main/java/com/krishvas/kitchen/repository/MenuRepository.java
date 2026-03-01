package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    Optional<Menu> findByScheduleDate(LocalDate scheduleDate);
    List<Menu> findByScheduleDateBetweenOrderByScheduleDateAsc(LocalDate start, LocalDate end);
    List<Menu> findByScheduleDateLessThanEqualOrderByScheduleDateDesc(LocalDate date);
    List<Menu> findTop15ByOrderByCreatedAtDesc();
}
