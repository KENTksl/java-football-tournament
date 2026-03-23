package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.repository.TeamRepository;

@Service
public class TeamService {
	private final TeamRepository teamRepository;

	public TeamService(TeamRepository teamRepository) {
		this.teamRepository = teamRepository;
	}

	@Transactional(readOnly = true)
	public List<Team> listTeams() {
		return teamRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<Team> findById(Long id) {
		return teamRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public Optional<Team> findByIdWithCaptain(Long id) {
		return teamRepository.findByIdWithCaptain(id);
	}

	@Transactional(readOnly = true)
	public Optional<Team> findByName(String name) {
		return teamRepository.findByNameIgnoreCase(name);
	}

	@Transactional(readOnly = true)
	public Optional<Team> findCaptainTeam(Long captainUserId) {
		return teamRepository.findFirstByCaptainId(captainUserId);
	}

	@Transactional(readOnly = true)
	public Optional<Team> findCaptainTeamWithCaptain(Long captainUserId) {
		if (captainUserId == null) return Optional.empty();
		List<Team> teams = teamRepository.findByCaptainIdWithCaptainOrderByCreatedAtDesc(captainUserId);
		if (teams == null || teams.isEmpty()) return Optional.empty();
		return Optional.ofNullable(teams.get(0));
	}

	@Transactional(readOnly = true)
	public List<Team> listByCaptainWithCaptain(Long captainUserId) {
		if (captainUserId == null) return List.of();
		List<Team> teams = teamRepository.findByCaptainIdWithCaptainOrderByCreatedAtDesc(captainUserId);
		return teams == null ? List.of() : teams;
	}

	@Transactional(readOnly = true)
	public List<Team> listByCaptain(Long captainUserId) {
		return teamRepository.findByCaptainIdOrderByCreatedAtDesc(captainUserId);
	}

	@Transactional(readOnly = true)
	public long countByCaptain(Long captainUserId) {
		return teamRepository.countByCaptainId(captainUserId);
	}

	@Transactional
	public Team save(Team team) {
		return teamRepository.save(team);
	}



	@Transactional(readOnly = true)
	public long countTeams() {
		return teamRepository.count();
	}
}

