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
	public Optional<Team> findByName(String name) {
		return teamRepository.findByNameIgnoreCase(name);
	}

	@Transactional
	public Team save(Team team) {
		return teamRepository.save(team);
	}
}

