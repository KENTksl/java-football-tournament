package com.example.football_tourament_web.model.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.football_tourament_web.model.enums.PitchType;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.model.enums.TournamentStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournaments")
public class Tournament {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	private String organizer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TournamentMode mode = TournamentMode.KNOCKOUT;

	@Enumerated(EnumType.STRING)
	private PitchType pitchType = PitchType.PITCH_7;

	private Integer teamLimit;

	private String imageUrl;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TournamentStatus status = TournamentStatus.UPCOMING;

	private LocalDate startDate;

	private LocalDate endDate;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TournamentRegistration> registrations = new ArrayList<>();

	@OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Match> matches = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "winner_id")
	private Team winner;

	public Tournament() {
	}

	public Tournament(String name) {
		this.name = name;
	}

	@PrePersist
	void prePersist() {
		var now = Instant.now();
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrganizer() {
		return organizer;
	}

	public void setOrganizer(String organizer) {
		this.organizer = organizer;
	}

	public TournamentMode getMode() {
		return mode;
	}

	public void setMode(TournamentMode mode) {
		this.mode = mode;
	}

	public PitchType getPitchType() {
		return pitchType;
	}

	public void setPitchType(PitchType pitchType) {
		this.pitchType = pitchType;
	}

	public Integer getTeamLimit() {
		return teamLimit;
	}

	public void setTeamLimit(Integer teamLimit) {
		this.teamLimit = teamLimit;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TournamentStatus getStatus() {
		return status;
	}

	public void setStatus(TournamentStatus status) {
		this.status = status;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Team getWinner() {
		return winner;
	}

	public void setWinner(Team winner) {
		this.winner = winner;
	}
}

