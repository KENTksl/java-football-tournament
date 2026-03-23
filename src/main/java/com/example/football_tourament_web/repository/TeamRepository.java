package com.example.football_tourament_web.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.football_tourament_web.model.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
	Optional<Team> findByNameIgnoreCase(String name);

	Optional<Team> findFirstByCaptainId(Long captainId);

	List<Team> findByCaptainIdOrderByCreatedAtDesc(Long captainId);

	long countByCaptainId(Long captainId);

	@Query("""
			select t
			from Team t
			left join fetch t.captain c
			where t.id = :id
			""")
	Optional<Team> findByIdWithCaptain(@Param("id") Long id);

	@Query("""
			select t
			from Team t
			left join fetch t.captain c
			where c.id = :captainId
			order by t.createdAt desc
			""")
	List<Team> findByCaptainIdWithCaptainOrderByCreatedAtDesc(@Param("captainId") Long captainId);
}

