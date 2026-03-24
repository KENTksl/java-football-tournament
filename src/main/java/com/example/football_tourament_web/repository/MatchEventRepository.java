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

	@Query("""
			select e
			from MatchEvent e
			left join fetch e.player p
			left join fetch p.team t
			where e.match.tournament.id = :tournamentId
			""")
	List<MatchEvent> findByTournamentId(@Param("tournamentId") Long tournamentId);

	@Query("""
			select count(e)
			from MatchEvent e
			where e.player.team.id = :teamId
			""")
	long countByPlayerTeamId(@Param("teamId") Long teamId);

	void deleteByMatchId(Long matchId);
}
