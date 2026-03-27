package com.example.football_tourament_web.repository;

import com.example.football_tourament_web.model.entity.MatchRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRatingRepository extends JpaRepository<MatchRating, Long> {
    List<MatchRating> findByMatchId(Long matchId);

    @Query("SELECT mr " +
           "FROM MatchRating mr " +
           "JOIN FETCH mr.player p " +
           "LEFT JOIN FETCH p.team " +
           "JOIN FETCH mr.match m " +
           "WHERE m.tournament.id = :tournamentId " +
           "ORDER BY m.scheduledAt DESC, m.id DESC, mr.rating DESC")
    List<MatchRating> findRatingsByTournamentId(@Param("tournamentId") Long tournamentId);
}
