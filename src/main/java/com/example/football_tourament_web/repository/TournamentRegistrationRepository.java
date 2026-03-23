package com.example.football_tourament_web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.enums.RegistrationStatus;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {
	List<TournamentRegistration> findByTournamentId(Long tournamentId);

	@Query("""
			select r
			from TournamentRegistration r
			join fetch r.team tm
			left join fetch tm.captain c
			where r.tournament.id = :tournamentId
			order by r.createdAt asc
			""")
	List<TournamentRegistration> findByTournamentIdWithTeam(@Param("tournamentId") Long tournamentId);

	@Query("""
			select count(distinct r.team.id)
			from TournamentRegistration r
			where r.tournament.id = :tournamentId and r.status <> :excludedStatus
			""")
	long countDistinctTeamByTournamentIdAndStatusNot(
			@Param("tournamentId") Long tournamentId,
			@Param("excludedStatus") RegistrationStatus excludedStatus
	);

	List<TournamentRegistration> findByRegisteredByIdOrderByCreatedAtDesc(Long userId);

	Optional<TournamentRegistration> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

	@Query("""
			select r
			from TournamentRegistration r
			join fetch r.tournament t
			join fetch r.team tm
			where r.registeredBy.id = :userId
			order by r.createdAt desc
			""")
	List<TournamentRegistration> findByRegisteredByIdWithDetails(@Param("userId") Long userId);

	@Query("""
			select r
			from TournamentRegistration r
			join fetch r.team tm
			left join fetch tm.captain c
			left join fetch r.tournament t
			where r.id = :id
			""")
	Optional<TournamentRegistration> findByIdWithTeamAndTournament(@Param("id") Long id);

	@Query("""
			select r
			from TournamentRegistration r
			join fetch r.tournament t
			where r.team.id = :teamId and r.status = :status
			order by r.createdAt desc
			""")
	List<TournamentRegistration> findByTeamIdAndStatusWithTournament(@Param("teamId") Long teamId, @Param("status") RegistrationStatus status);
}

