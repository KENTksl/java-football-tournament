package com.example.football_tourament_web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.TournamentRegistration;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {
	List<TournamentRegistration> findByTournamentId(Long tournamentId);

	List<TournamentRegistration> findByRegisteredByIdOrderByCreatedAtDesc(Long userId);

	Optional<TournamentRegistration> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);
}

