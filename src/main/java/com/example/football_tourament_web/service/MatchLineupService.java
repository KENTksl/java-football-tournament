package com.example.football_tourament_web.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.MatchLineupSlot;
import com.example.football_tourament_web.repository.MatchLineupSlotRepository;

@Service
public class MatchLineupService {
	private final MatchLineupSlotRepository slotRepository;

	public MatchLineupService(MatchLineupSlotRepository slotRepository) {
		this.slotRepository = slotRepository;
	}

	@Transactional(readOnly = true)
	public List<MatchLineupSlot> listByMatchId(Long matchId) {
		if (matchId == null) return List.of();
		return slotRepository.findByMatchIdWithPlayerOrderByTeamSideAscSlotIndexAsc(matchId);
	}

	@Transactional
	public void replaceLineup(Long matchId, List<MatchLineupSlot> slots) {
		if (matchId == null) return;
		slotRepository.deleteByMatchId(matchId);
		slotRepository.flush();
		if (slots == null || slots.isEmpty()) return;
		slotRepository.saveAll(slots);
		slotRepository.flush();
	}
}
