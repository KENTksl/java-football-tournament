package com.example.football_tourament_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.Match;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MatchRepository extends JpaRepository<Match, Long> {
	List<Match> findByTournamentIdOrderByScheduledAtAsc(Long tournamentId);

	@Query("SELECT COUNT(m) FROM Match m WHERE m.scheduledAt >= :start AND m.scheduledAt < :end")
	long countMatchesByScheduledAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

