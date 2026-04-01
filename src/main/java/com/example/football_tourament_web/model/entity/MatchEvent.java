package com.example.football_tourament_web.model.entity;

import java.time.Instant;

import com.example.football_tourament_web.model.enums.MatchEventType;
import com.example.football_tourament_web.model.enums.TeamSide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "match_events")
public class MatchEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", nullable = false)
	private Match match;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TeamSide teamSide;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id")
	private Player player;

	@Column(nullable = false)
	private Integer minute;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MatchEventType type;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public MatchEvent() {
	}

	public MatchEvent(Match match, TeamSide teamSide, Player player, Integer minute, MatchEventType type) {
		this.match = match;
		this.teamSide = teamSide;
		this.player = player;
		this.minute = minute;
		this.type = type;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Match getMatch() {
		return match;
	}

	public void setMatch(Match match) {
		this.match = match;
	}

	public TeamSide getTeamSide() {
		return teamSide;
	}

	public void setTeamSide(TeamSide teamSide) {
		this.teamSide = teamSide;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public Integer getMinute() {
		return minute;
	}

	public void setMinute(Integer minute) {
		this.minute = minute;
	}

	public MatchEventType getType() {
		return type;
	}

	public void setType(MatchEventType type) {
		this.type = type;
	}
}

