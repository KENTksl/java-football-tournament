package com.example.football_tourament_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.football_tourament_web.model.entity.MatchEvent;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
	List<MatchEvent> findByMatchIdOrderByMinuteAscIdAsc(Long matchId);

	@Query("""
			select e
			from MatchEvent e
			left join fetch e.player p
			where e.match.id = :matchId
			order by e.minute asc, e.id asc
			""")
	List<MatchEvent> findByMatchIdWithPlayerOrderByMinuteAscIdAsc(@Param("matchId") Long matchId);

	void deleteByMatchId(Long matchId);
}
