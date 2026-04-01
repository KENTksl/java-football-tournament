package com.example.football_tourament_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.Query;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.enums.TournamentStatus;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
	long countByStatus(TournamentStatus status);

	@Query("SELECT t FROM Tournament t LEFT JOIN FETCH t.winner WHERE t.winner IS NOT NULL ORDER BY t.id DESC")
	List<Tournament> findRecentWinners();
}

