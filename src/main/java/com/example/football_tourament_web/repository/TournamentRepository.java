package com.example.football_tourament_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.enums.TournamentStatus;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
	long countByStatus(TournamentStatus status);

	List<Tournament> findTop4ByWinnerIsNotNullOrderByIdDesc();
}

