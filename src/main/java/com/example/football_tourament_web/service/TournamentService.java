package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.repository.TournamentRepository;

@Service
public class TournamentService {
	private final TournamentRepository tournamentRepository;

	public TournamentService(TournamentRepository tournamentRepository) {
		this.tournamentRepository = tournamentRepository;
	}

	@Transactional(readOnly = true)
	public List<Tournament> listTournaments() {
		return tournamentRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<Tournament> findById(Long id) {
		return tournamentRepository.findById(id);
	}

	@Transactional
	public Tournament save(Tournament tournament) {
		return tournamentRepository.save(tournament);
	}

	@Transactional
	public void deleteById(Long id) {
		tournamentRepository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public long countTournaments() {
		return tournamentRepository.count();
	}

	@Transactional(readOnly = true)
	public long countTournamentsByStatus(TournamentStatus status) {
		return tournamentRepository.countByStatus(status);
	}

	@Transactional(readOnly = true)
	public List<Tournament> getRecentWinners() {
		return tournamentRepository.findTop4ByWinnerIsNotNullOrderByIdDesc();
	}
}

