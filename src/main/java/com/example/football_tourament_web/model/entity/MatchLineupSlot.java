package com.example.football_tourament_web.model.entity;

import java.time.Instant;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "match_lineup_slots",
		uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "team_side", "slot_index"})
)
public class MatchLineupSlot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", nullable = false)
	private Match match;

	@Enumerated(EnumType.STRING)
	@Column(name = "team_side", nullable = false)
	private TeamSide teamSide;

	@Column(name = "slot_index", nullable = false)
	private Integer slotIndex;

	@Column(nullable = false)
	private String position;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public MatchLineupSlot() {
	}

	public MatchLineupSlot(Match match, TeamSide teamSide, Integer slotIndex, String position, Player player) {
		this.match = match;
		this.teamSide = teamSide;
		this.slotIndex = slotIndex;
		this.position = position;
		this.player = player;
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

	public Integer getSlotIndex() {
		return slotIndex;
	}

	public void setSlotIndex(Integer slotIndex) {
		this.slotIndex = slotIndex;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}
}

