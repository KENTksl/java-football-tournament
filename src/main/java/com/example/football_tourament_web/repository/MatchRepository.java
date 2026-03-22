package com.example.football_tourament_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
	List<Match> findByTournamentIdOrderByScheduledAtAsc(Long tournamentId);
}

