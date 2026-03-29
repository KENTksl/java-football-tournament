package com.example.football_tourament_web.repository;

import com.example.football_tourament_web.model.entity.MatchVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchVoteRepository extends JpaRepository<MatchVote, Long> {
    Optional<MatchVote> findByMatchIdAndUserId(Long matchId, Long userId);

    @Query("SELECT mv.player, COUNT(mv) as voteCount " +
           "FROM MatchVote mv " +
           "WHERE mv.match.id = :matchId " +
           "GROUP BY mv.player " +
           "ORDER BY voteCount DESC")
    List<Object[]> findVotesByMatchId(@Param("matchId") Long matchId);
}
