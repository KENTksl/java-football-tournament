package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.repository.TournamentRegistrationRepository;

@Service
public class TournamentRegistrationService {
	private final TournamentRegistrationRepository registrationRepository;

	public TournamentRegistrationService(TournamentRegistrationRepository registrationRepository) {
		this.registrationRepository = registrationRepository;
	}

	@Transactional(readOnly = true)
	public List<TournamentRegistration> listByTournamentId(Long tournamentId) {
		return registrationRepository.findByTournamentId(tournamentId);
	}

	@Transactional(readOnly = true)
	public List<TournamentRegistration> listByTournamentIdWithTeam(Long tournamentId) {
		return registrationRepository.findByTournamentIdWithTeam(tournamentId);
	}

	@Transactional(readOnly = true)
	public List<TournamentRegistration> listApprovedByTournamentIdWithTeam(Long tournamentId) {
		if (tournamentId == null) return List.of();
		return registrationRepository.findByTournamentIdWithTeamAndStatus(tournamentId, RegistrationStatus.APPROVED);
	}

	@Transactional(readOnly = true)
	public long countRegisteredTeams(Long tournamentId) {
		if (tournamentId == null) return 0;
		return registrationRepository.countDistinctTeamByTournamentIdAndStatus(tournamentId, RegistrationStatus.APPROVED);
	}

	@Transactional(readOnly = true)
	public List<TournamentRegistration> listByUserId(Long userId) {
		return registrationRepository.findByRegisteredByIdWithDetails(userId);
	}

	@Transactional(readOnly = true)
	public Optional<TournamentRegistration> findByTournamentAndTeam(Long tournamentId, Long teamId) {
		return registrationRepository.findByTournamentIdAndTeamId(tournamentId, teamId);
	}

	@Transactional(readOnly = true)
	public Optional<TournamentRegistration> findById(Long id) {
		return registrationRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public Optional<TournamentRegistration> findByIdWithDetails(Long id) {
		return registrationRepository.findByIdWithTeamAndTournament(id);
	}

	@Transactional(readOnly = true)
	public List<TournamentRegistration> listApprovedByTeamId(Long teamId) {
		return registrationRepository.findByTeamIdAndStatusWithTournament(teamId, RegistrationStatus.APPROVED);
	}

	@Transactional(readOnly = true)
	public List<TournamentRegistration> listRecentPendingWithDetails(int limit) {
		List<TournamentRegistration> items = registrationRepository.findByStatusWithDetailsOrderByCreatedAtDesc(RegistrationStatus.PENDING);
		if (items == null) return List.of();
		if (limit <= 0) return List.of();
		if (items.size() <= limit) return items;
		return items.subList(0, limit);
	}

	@Transactional(readOnly = true)
	public long countPending() {
		return registrationRepository.countByStatus(RegistrationStatus.PENDING);
	}

	@Transactional
	public TournamentRegistration save(TournamentRegistration registration) {
		return registrationRepository.save(registration);
	}
}

