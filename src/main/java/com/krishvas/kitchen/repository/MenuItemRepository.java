package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}
