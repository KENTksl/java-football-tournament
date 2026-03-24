package com.example.football_tourament_web.model.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.football_tourament_web.model.enums.RegistrationStatus;

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
	name = "tournament_registrations",
	uniqueConstraints = @UniqueConstraint(name = "uk_registration_tournament_team", columnNames = {"tournament_id", "team_id"})
)
public class TournamentRegistration {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tournament_id", nullable = false)
	private Tournament tournament;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "registered_by_user_id")
	private AppUser registeredBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RegistrationStatus status = RegistrationStatus.PENDING;

	@Column(name = "group_name")
	private String groupName;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	private String paidTransactionCode;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal refundedAmount = BigDecimal.ZERO;

	private String refundTransactionCode;

	private Instant refundedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public TournamentRegistration() {
	}

	public TournamentRegistration(Tournament tournament, Team team) {
		this.tournament = tournament;
		this.team = team;
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

	public Tournament getTournament() {
		return tournament;
	}

	public void setTournament(Tournament tournament) {
		this.tournament = tournament;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public AppUser getRegisteredBy() {
		return registeredBy;
	}

	public void setRegisteredBy(AppUser registeredBy) {
		this.registeredBy = registeredBy;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void setStatus(RegistrationStatus status) {
		this.status = status;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public BigDecimal getPaidAmount() {
		return paidAmount;
	}

	public void setPaidAmount(BigDecimal paidAmount) {
		this.paidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
	}

	public String getPaidTransactionCode() {
		return paidTransactionCode;
	}

	public void setPaidTransactionCode(String paidTransactionCode) {
		this.paidTransactionCode = paidTransactionCode;
	}

	public BigDecimal getRefundedAmount() {
		return refundedAmount;
	}

	public void setRefundedAmount(BigDecimal refundedAmount) {
		this.refundedAmount = refundedAmount == null ? BigDecimal.ZERO : refundedAmount;
	}

	public String getRefundTransactionCode() {
		return refundTransactionCode;
	}

	public void setRefundTransactionCode(String refundTransactionCode) {
		this.refundTransactionCode = refundTransactionCode;
	}

	public Instant getRefundedAt() {
		return refundedAt;
	}

	public void setRefundedAt(Instant refundedAt) {
		this.refundedAt = refundedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

