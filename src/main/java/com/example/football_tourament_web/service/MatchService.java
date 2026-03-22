package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.repository.MatchRepository;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;

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

	@Transactional(readOnly = true)
	public List<Long> getMatchFrequencyForLast7Months() {
		List<Long> frequency = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();

		for (int i = 6; i >= 0; i--) {
			YearMonth ym = YearMonth.from(now).minusMonths(i);
			LocalDateTime start = ym.atDay(1).atStartOfDay();
			LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
			frequency.add(matchRepository.countMatchesByScheduledAtBetween(start, end));
		}
		return frequency;
	}
}

