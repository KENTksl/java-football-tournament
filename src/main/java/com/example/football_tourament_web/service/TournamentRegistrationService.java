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
	public List<TournamentRegistration> listByUserId(Long userId) {
		return registrationRepository.findByRegisteredByIdWithDetails(userId);
	}

	@Transactional(readOnly = true)
	public Optional<TournamentRegistration> findByTournamentAndTeam(Long tournamentId, Long teamId) {
		return registrationRepository.findByTournamentIdAndTeamId(tournamentId, teamId);
	}

	@Transactional(readOnly = true)
	public List<TournamentRegistration> listApprovedByTeamId(Long teamId) {
		return registrationRepository.findByTeamIdAndStatusWithTournament(teamId, RegistrationStatus.APPROVED);
	}

	@Transactional
	public TournamentRegistration save(TournamentRegistration registration) {
		return registrationRepository.save(registration);
	}
}

