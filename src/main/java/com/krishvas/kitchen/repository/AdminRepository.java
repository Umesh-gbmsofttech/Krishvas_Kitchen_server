package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.Admin;
import com.krishvas.kitchen.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUser(User user);
}
