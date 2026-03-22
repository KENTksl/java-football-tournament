package com.example.football_tourament_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}

