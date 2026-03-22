package com.example.football_tourament_web.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
	Optional<Team> findByNameIgnoreCase(String name);

	Optional<Team> findFirstByCaptainId(Long captainId);

	List<Team> findByCaptainIdOrderByCreatedAtDesc(Long captainId);

	long countByCaptainId(Long captainId);
}

