package com.example.football_tourament_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
	List<Player> findByTeamIdOrderByJerseyNumberAsc(Long teamId);

	long countByTeamId(Long teamId);

	long deleteByTeamId(Long teamId);
}

