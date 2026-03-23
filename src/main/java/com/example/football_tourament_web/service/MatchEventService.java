package com.example.football_tourament_web.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.MatchEvent;
import com.example.football_tourament_web.repository.MatchEventRepository;

@Service
public class MatchEventService {
	private final MatchEventRepository matchEventRepository;

	public MatchEventService(MatchEventRepository matchEventRepository) {
		this.matchEventRepository = matchEventRepository;
	}

	@Transactional(readOnly = true)
	public List<MatchEvent> listByMatchId(Long matchId) {
		if (matchId == null) return List.of();
		return matchEventRepository.findByMatchIdWithPlayerOrderByMinuteAscIdAsc(matchId);
	}

	@Transactional
	public void replaceMatchEvents(Long matchId, List<MatchEvent> events) {
		if (matchId == null) return;
		matchEventRepository.deleteByMatchId(matchId);
		if (events == null || events.isEmpty()) return;
		matchEventRepository.saveAll(events);
	}

	@Transactional
	public MatchEvent save(MatchEvent event) {
		if (event == null) return null;
		return matchEventRepository.save(event);
	}

	@Transactional
	public void deleteById(Long id) {
		if (id == null) return;
		matchEventRepository.deleteById(id);
	}
}
