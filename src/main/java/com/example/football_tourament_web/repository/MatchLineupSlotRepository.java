package com.example.football_tourament_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.football_tourament_web.model.entity.MatchLineupSlot;

public interface MatchLineupSlotRepository extends JpaRepository<MatchLineupSlot, Long> {
	List<MatchLineupSlot> findByMatchIdOrderByTeamSideAscSlotIndexAsc(Long matchId);

	@Query("""
			select s
			from MatchLineupSlot s
			join fetch s.player p
			where s.match.id = :matchId
			order by s.teamSide asc, s.slotIndex asc
			""")
	List<MatchLineupSlot> findByMatchIdWithPlayerOrderByTeamSideAscSlotIndexAsc(@Param("matchId") Long matchId);

	void deleteByMatchId(Long matchId);
}
