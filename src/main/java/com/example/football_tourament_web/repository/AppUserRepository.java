package com.example.football_tourament_web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.enums.UserRole;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
	Optional<AppUser> findByEmail(String email);
	List<AppUser> findByRoleOrderByCreatedAtDesc(UserRole role);
}

