package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.repository.MatchRepository;

@Service
public class MatchService {
	private final MatchRepository matchRepository;

	public MatchService(MatchRepository matchRepository) {
		this.matchRepository = matchRepository;
	}

	@Transactional(readOnly = true)
	public List<Match> listByTournamentId(Long tournamentId) {
		return matchRepository.findByTournamentIdOrderByScheduledAtAsc(tournamentId);
	}

	@Transactional(readOnly = true)
	public Optional<Match> findById(Long id) {
		return matchRepository.findById(id);
	}

	@Transactional
	public Match save(Match match) {
		return matchRepository.save(match);
	}
}

