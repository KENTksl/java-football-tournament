package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.MatchEvent;
import com.example.football_tourament_web.model.entity.MatchLineupSlot;
import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.MatchEventType;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.PitchType;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TeamSide;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.model.enums.UserRole;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.core.MatchEventService;
import com.example.football_tourament_web.service.core.MatchLineupService;
import com.example.football_tourament_web.service.core.MatchService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TournamentService;
import com.example.football_tourament_web.service.core.TransactionService;
import com.example.football_tourament_web.service.core.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminMatchHistoryService {
	private final TournamentService tournamentService;
	private final MatchService matchService;
	private final MatchLineupService matchLineupService;
	private final MatchEventService matchEventService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final PlayerRepository playerRepository;
	private final TransactionService transactionService;
	private final UserService userService;

	public AdminMatchHistoryService(
			TournamentService tournamentService,
			MatchService matchService,
			MatchLineupService matchLineupService,
			MatchEventService matchEventService,
			TournamentRegistrationService tournamentRegistrationService,
			PlayerRepository playerRepository,
			TransactionService transactionService,
			UserService userService
	) {
		this.tournamentService = tournamentService;
		this.matchService = matchService;
		this.matchLineupService = matchLineupService;
		this.matchEventService = matchEventService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.playerRepository = playerRepository;
		this.transactionService = transactionService;
		this.userService = userService;
	}

	public PageResult buildMatchHistoryPage(Long tournamentId, Long matchId, String tab, int page, int size) {
		if (tournamentId == null) return PageResult.redirect("/admin/manage/tournament");
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) return PageResult.redirect("/admin/manage/tournament");

		Map<String, Object> model = new HashMap<>();
		applyTournamentContext(model, tournament);
		model.put("tournamentMode", tournament.getMode());
		model.put("tournamentTeamLimit", tournament.getTeamLimit());
		model.put("tournamentPitchType", tournament.getPitchType());
		model.put("tournamentStartDate", tournament.getStartDate());
		model.put("tournamentEndDate", tournament.getEndDate());

		List<Match> allMatches = updateMatchStatusesByTime(matchService.listByTournamentIdWithDetails(tournamentId));
		List<Match> knockoutOnly = new ArrayList<>();
		for (Match m : allMatches) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim().toLowerCase();
			if (rn.startsWith("bảng")) continue;
			knockoutOnly.add(m);
		}

		List<Match> knockoutSource = tournament.getMode() == TournamentMode.KNOCKOUT ? allMatches : knockoutOnly;
		PagedResult<Match> paged = paginate(knockoutSource, page, size);
		model.put("knockoutMatches", paged.getItems());
		model.put("currentPage", paged.getCurrentPage());
		model.put("pageSize", paged.getPageSize());
		model.put("totalPages", paged.getTotalPages());
		model.put("pairingLocked", !allMatches.isEmpty());

		if (tournament.getMode() == TournamentMode.GROUP_STAGE) {
			applyGroupStageModel(model, tournament, tournamentId, allMatches, knockoutOnly, page, size);
		}

		if (matchId != null) {
			applySelectedMatchModel(model, tournamentId, matchId, tab, tournament);
		}

		return PageResult.view("admin/tournament/match-history", model);
	}

	public String randomGroups(Long tournamentId) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) return "redirect:/admin/manage/tournament";

		if (tournament.getMode() != TournamentMode.GROUP_STAGE) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Integer teamLimit = tournament.getTeamLimit();
		if (teamLimit == null || teamLimit != 16) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		List<TournamentRegistration> registrations = tournamentRegistrationService.listApprovedByTournamentIdWithTeam(tournamentId);
		List<TournamentRegistration> uniqueRegs = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration r : registrations) {
			if (r == null) continue;
			if (r.getStatus() != RegistrationStatus.APPROVED) continue;
			if (r.getTeam() == null || r.getTeam().getId() == null) continue;
			Long teamId = r.getTeam().getId();
			if (seenTeamIds.contains(teamId)) continue;
			seenTeamIds.add(teamId);
			uniqueRegs.add(r);
		}

		if (uniqueRegs.size() != teamLimit) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		boolean alreadyAssigned = uniqueRegs.stream().anyMatch(r -> r.getGroupName() != null && !r.getGroupName().trim().isBlank());
		if (alreadyAssigned) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Collections.shuffle(uniqueRegs);
		String[] groups = new String[]{"A", "B", "C", "D"};
		for (int i = 0; i < uniqueRegs.size(); i++) {
			TournamentRegistration r = uniqueRegs.get(i);
			r.setGroupName(groups[i % 4]);
			tournamentRegistrationService.save(r);
		}
		matchService.generateGroupStageMatchesIfMissing(tournament, uniqueRegs);

		return "redirect:/admin/match-history?id=" + tournamentId + "&saved=groups";
	}

	public MatchPlayersResponse matchPlayers(Long tournamentId, Long matchId) {
		if (tournamentId == null || matchId == null) {
			return new MatchPlayersResponse(List.of(), List.of(), null, null);
		}
		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return new MatchPlayersResponse(List.of(), List.of(), null, null);
		}

		Long homeTeamId = match.getHomeTeam() == null ? null : match.getHomeTeam().getId();
		Long awayTeamId = match.getAwayTeam() == null ? null : match.getAwayTeam().getId();

		List<PlayerDto> homePlayers = new ArrayList<>();
		List<PlayerDto> awayPlayers = new ArrayList<>();

		if (homeTeamId != null) {
			for (Player p : playerRepository.findByTeamIdOrderByJerseyNumberAsc(homeTeamId)) {
				homePlayers.add(new PlayerDto(p.getId(), p.getFullName(), p.getJerseyNumber(), p.getPosition(), p.getAvatarUrl()));
			}
		}
		if (awayTeamId != null) {
			for (Player p : playerRepository.findByTeamIdOrderByJerseyNumberAsc(awayTeamId)) {
				awayPlayers.add(new PlayerDto(p.getId(), p.getFullName(), p.getJerseyNumber(), p.getPosition(), p.getAvatarUrl()));
			}
		}

		return new MatchPlayersResponse(homePlayers, awayPlayers, match.getLineupJson(), match.getEventsJson());
	}

	public void saveLineupJson(Long tournamentId, Long matchId, String lineupJson) {
		if (tournamentId == null || matchId == null) return;
		Match match = matchService.findById(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return;
		}

		String payload = lineupJson == null ? null : lineupJson.trim();
		if (payload != null && payload.length() > 20000) {
			payload = payload.substring(0, 20000);
		}
		match.setLineupJson(payload == null || payload.isBlank() ? null : payload);
		matchService.save(match);
	}

	public void saveEventsJson(Long tournamentId, Long matchId, String eventsJson) {
		if (tournamentId == null || matchId == null) return;
		Match match = matchService.findById(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return;
		}

		String payload = eventsJson == null ? null : eventsJson.trim();
		if (payload != null && payload.length() > 20000) {
			payload = payload.substring(0, 20000);
		}
		match.setEventsJson(payload == null || payload.isBlank() ? null : payload);
		matchService.save(match);
	}

	public String randomPair(Long tournamentId) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) return "redirect:/admin/manage/tournament";

		if (tournament.getMode() != TournamentMode.KNOCKOUT) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Integer teamLimit = tournament.getTeamLimit();
		if (teamLimit == null || !(teamLimit == 4 || teamLimit == 8 || teamLimit == 16)) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		if (matchService.countByTournamentId(tournamentId) > 0) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		List<TournamentRegistration> registrations = tournamentRegistrationService.listApprovedByTournamentIdWithTeam(tournamentId);
		List<Team> teams = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;
			if (registration.getStatus() != RegistrationStatus.APPROVED) continue;
			if (registration.getTeam() == null || registration.getTeam().getId() == null) continue;
			Long teamId = registration.getTeam().getId();
			if (seenTeamIds.contains(teamId)) continue;
			seenTeamIds.add(teamId);
			teams.add(registration.getTeam());
		}

		if (teams.size() != teamLimit) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Collections.shuffle(teams);
		String roundName = firstRoundName(teamLimit);

		List<Match> matches = new ArrayList<>();
		for (int i = 0; i < teams.size(); i += 2) {
			Team home = teams.get(i);
			Team away = teams.get(i + 1);
			Match match = new Match(tournament, home, away);
			match.setRoundName(roundName);
			match.setStatus(MatchStatus.SCHEDULED);
			matches.add(match);
		}

		matchService.saveAll(matches);
		return "redirect:/admin/match-history?id=" + tournamentId;
	}

	public String saveSchedule(Long tournamentId, Long matchId, String date, String time, String location, int page, int size) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		if (matchId == null) return "redirect:/admin/match-history?id=" + tournamentId;

		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) return "redirect:/admin/match-history?id=" + tournamentId;

		String nextLocation = location == null ? "" : location.trim();
		match.setLocation(nextLocation.isBlank() ? null : nextLocation);

		LocalDateTime scheduledAt = null;
		try {
			String d = date == null ? "" : date.trim();
			String t = time == null ? "" : time.trim();
			if (!d.isBlank() && !t.isBlank()) {
				LocalDate selectedDate = LocalDate.parse(d);
				LocalDate start = tournament.getStartDate();
				LocalDate end = tournament.getEndDate();
				if (start != null && selectedDate.isBefore(start)) {
					return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=invalid_schedule";
				}
				if (end != null && selectedDate.isAfter(end)) {
					return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=invalid_schedule";
				}
				scheduledAt = selectedDate.atTime(LocalTime.parse(t));
			}
		} catch (Exception ignored) {
			scheduledAt = null;
		}

		match.setScheduledAt(scheduledAt);
		matchService.save(match);

		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=schedule";
	}

	@Transactional
	public String saveScore(Long tournamentId, Long matchId, Integer homeScore, Integer awayScore, Integer homePen, Integer awayPen, int page, int size) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		if (matchId == null) return "redirect:/admin/match-history?id=" + tournamentId;

		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		boolean isKnockout = match.getTournament() != null && match.getTournament().getMode() != TournamentMode.GROUP_STAGE;
		boolean isGroupRound = match.getRoundName() != null && match.getRoundName().trim().toLowerCase().startsWith("bảng");

		if (isKnockout && !isGroupRound && homeScore != null && awayScore != null && homeScore.equals(awayScore)) {
			if (homePen != null && awayPen != null && homePen.equals(awayPen)) {
				return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=pen_invalid";
			}
		}

		match.setHomeScore(homeScore);
		match.setAwayScore(awayScore);
		if (homeScore != null && awayScore != null && !homeScore.equals(awayScore)) {
			match.setHomePenalty(null);
			match.setAwayPenalty(null);
		} else {
			match.setHomePenalty(homePen);
			match.setAwayPenalty(awayPen);
		}
		matchService.save(match);

		if (match.getScheduledAt() == null) {
			return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=need_schedule";
		}

		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=score";
	}

	public String saveLineupForm(Long tournamentId, Long matchId, Map<String, String> params, int page, int size) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		if (matchId == null) return "redirect:/admin/match-history?id=" + tournamentId;

		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		List<PitchSlot> pitchSlots = buildPitchSlots(match.getTournament() == null ? null : match.getTournament().getPitchType());
		List<MatchLineupSlot> slots = new ArrayList<>();
		Long homeTeamId = match.getHomeTeam() == null ? null : match.getHomeTeam().getId();
		Long awayTeamId = match.getAwayTeam() == null ? null : match.getAwayTeam().getId();

		for (PitchSlot slotDef : pitchSlots) {
			int index = slotDef.index();
			String homeKey = "homeSlotPlayerIds[" + index + "]";
			String awayKey = "awaySlotPlayerIds[" + index + "]";

			Long homePlayerId = parseLongOrNull(params.get(homeKey));
			Long awayPlayerId = parseLongOrNull(params.get(awayKey));

			if (homePlayerId != null && homeTeamId != null) {
				Player p = playerRepository.findById(homePlayerId).orElse(null);
				if (p != null && p.getTeam() != null && homeTeamId.equals(p.getTeam().getId())) {
					slots.add(new MatchLineupSlot(match, TeamSide.HOME, index, slotDef.label(), p));
				}
			}
			if (awayPlayerId != null && awayTeamId != null) {
				Player p = playerRepository.findById(awayPlayerId).orElse(null);
				if (p != null && p.getTeam() != null && awayTeamId.equals(p.getTeam().getId())) {
					slots.add(new MatchLineupSlot(match, TeamSide.AWAY, index, slotDef.label(), p));
				}
			}
		}

		matchLineupService.replaceLineup(matchId, slots);
		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=lineup";
	}

	public String addEvent(Long tournamentId, Long matchId, TeamSide teamSide, Integer minute, MatchEventType type, Long playerId, int page, int size) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		if (matchId == null) return "redirect:/admin/match-history?id=" + tournamentId;

		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}
		if (teamSide == null || type == null) {
			return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=timeline&page=" + page + "&size=" + size;
		}
		int safeMinute = minute == null ? 0 : Math.max(0, minute);

		Player player = null;
		if (playerId != null) {
			Player p = playerRepository.findById(playerId).orElse(null);
			Long expectedTeamId = teamSide == TeamSide.AWAY ? (match.getAwayTeam() == null ? null : match.getAwayTeam().getId())
					: (match.getHomeTeam() == null ? null : match.getHomeTeam().getId());
			if (p != null && expectedTeamId != null && p.getTeam() != null && expectedTeamId.equals(p.getTeam().getId())) {
				player = p;
			}
		}

		matchEventService.save(new MatchEvent(match, teamSide, player, safeMinute, type));
		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=timeline&page=" + page + "&size=" + size + "&saved=event";
	}

	public String deleteEvent(Long tournamentId, Long matchId, Long eventId, int page, int size) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		if (matchId == null) return "redirect:/admin/match-history?id=" + tournamentId;
		if (eventId != null) {
			matchEventService.deleteById(eventId);
		}
		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=timeline&page=" + page + "&size=" + size + "&saved=event";
	}

	@Transactional
	public String syncScoreFromEvents(Long tournamentId, Long matchId, int page, int size) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		if (matchId == null) return "redirect:/admin/match-history?id=" + tournamentId;
		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		int homeGoals = 0;
		int awayGoals = 0;
		for (MatchEvent ev : matchEventService.listByMatchId(matchId)) {
			if (ev == null || ev.getType() != MatchEventType.GOAL || ev.getTeamSide() == null) continue;
			if (ev.getTeamSide() == TeamSide.AWAY) awayGoals++;
			if (ev.getTeamSide() == TeamSide.HOME) homeGoals++;
		}
		match.setHomeScore(homeGoals);
		match.setAwayScore(awayGoals);
		matchService.save(match);

		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=score";
	}

	@Transactional
	public String finishMatch(Long tournamentId, Long matchId, int page, int size) {
		if (tournamentId == null) return "redirect:/admin/manage/tournament";
		if (matchId == null) return "redirect:/admin/match-history?id=" + tournamentId;

		Match match = matchService.findById(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		if (match.getHomeScore() == null || match.getAwayScore() == null) {
			return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=score_required";
		}

		// Knockout tie requires penalty and cannot be equal
		boolean isGroupRound = match.getRoundName() != null && match.getRoundName().trim().toLowerCase().startsWith("bảng");
		boolean isKnockoutMatch = (match.getTournament() != null && match.getTournament().getMode() == TournamentMode.KNOCKOUT) || !isGroupRound;

		if (isKnockoutMatch && match.getHomeScore().equals(match.getAwayScore())) {
			if (match.getHomePenalty() == null || match.getAwayPenalty() == null) {
				return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=pen_required";
			}
			if (match.getHomePenalty().equals(match.getAwayPenalty())) {
				return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=pen_invalid";
			}
		}

		if (match.getScheduledAt() == null) {
			return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=need_schedule";
		}

		match.setStatus(MatchStatus.FINISHED);
		matchService.save(match);

		if ("Chung kết".equalsIgnoreCase(match.getRoundName() != null ? match.getRoundName().trim() : "")) {
			Team winner = matchService.winnerOf(match);
			if (winner != null) {
				Tournament tournament = match.getTournament();
				tournament.setWinner(winner);
				tournament.setStatus(TournamentStatus.FINISHED);
				distributePrizesIfNeeded(tournament, match);
				tournamentService.save(tournament);
			}
		}

		matchService.generateNextKnockoutRoundIfReady(tournamentId, match.getRoundName());
		matchService.generateQuarterFinalsFromGroupsIfReady(tournamentId);
		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=finish";
	}

	private void distributePrizesIfNeeded(Tournament tournament, Match finalMatch) {
		if (tournament == null || tournament.getId() == null) return;
		if (tournament.getPrizeDistributedAt() != null) return;
		BigDecimal pool = tournament.getPrizePool() == null ? BigDecimal.ZERO : tournament.getPrizePool();
		if (pool.signum() <= 0) return;
		if (finalMatch == null || finalMatch.getHomeTeam() == null || finalMatch.getAwayTeam() == null) return;

		Team winnerTeam = matchService.winnerOf(finalMatch);
		if (winnerTeam == null || winnerTeam.getId() == null) return;
		Team runnerUpTeam = winnerTeam.getId().equals(finalMatch.getHomeTeam().getId()) ? finalMatch.getAwayTeam() : finalMatch.getHomeTeam();

		if (winnerTeam.getCaptain() == null || winnerTeam.getCaptain().getId() == null) return;
		if (runnerUpTeam == null || runnerUpTeam.getCaptain() == null || runnerUpTeam.getCaptain().getId() == null) return;
		var admins = userService.listUsersByRole(UserRole.ADMIN);
		if (admins == null || admins.isEmpty() || admins.get(0) == null || admins.get(0).getId() == null) return;
		var admin = admins.get(0);

		BigDecimal winnerAmount = pool.multiply(new BigDecimal("0.35")).setScale(2, java.math.RoundingMode.DOWN);
		BigDecimal runnerUpAmount = pool.multiply(new BigDecimal("0.15")).setScale(2, java.math.RoundingMode.DOWN);
		BigDecimal adminAmount = pool.subtract(winnerAmount).subtract(runnerUpAmount);
		if (adminAmount.signum() < 0) return;

		String baseCode = "PRIZE_" + tournament.getId() + "_" + UUID.randomUUID();
		transactionService.save(buildPrizeTx(baseCode + "_WIN", winnerTeam.getCaptain(), winnerAmount, "Thưởng vô địch giải đấu: " + safeTournamentName(tournament)));
		transactionService.save(buildPrizeTx(baseCode + "_RUN", runnerUpTeam.getCaptain(), runnerUpAmount, "Thưởng á quân giải đấu: " + safeTournamentName(tournament)));
		transactionService.save(buildPrizeTx(baseCode + "_ADM", admin, adminAmount, "Chia thưởng admin giải đấu: " + safeTournamentName(tournament)));

		tournament.setPrizeDistributedAt(Instant.now());
	}

	private Transaction buildPrizeTx(String code, AppUser user, BigDecimal amount, String description) {
		Transaction tx = new Transaction(code, description, amount, user);
		tx.setStatus(TransactionStatus.SUCCESS);
		return tx;
	}

	private String safeTournamentName(Tournament tournament) {
		if (tournament == null) return "";
		String name = tournament.getName();
		if (name != null && !name.isBlank()) return name;
		return "#" + tournament.getId();
	}

	private void applyGroupStageModel(
			Map<String, Object> model,
			Tournament tournament,
			Long tournamentId,
			List<Match> allMatches,
			List<Match> knockoutOnly,
			int page,
			int size
	) {
		Map<String, List<GroupTeamRow>> groupTeams = new HashMap<>();
		groupTeams.put("A", new ArrayList<>());
		groupTeams.put("B", new ArrayList<>());
		groupTeams.put("C", new ArrayList<>());
		groupTeams.put("D", new ArrayList<>());

		List<TournamentRegistration> registrations = tournamentRegistrationService.listApprovedByTournamentIdWithTeam(tournamentId);
		List<Long> seenTeamIds = new ArrayList<>();
		boolean allAssigned = true;
		int teamCount = 0;

		for (TournamentRegistration r : registrations) {
			if (r == null) continue;
			if (r.getStatus() != RegistrationStatus.APPROVED) continue;
			if (r.getTeam() == null || r.getTeam().getId() == null) continue;

			Long teamId = r.getTeam().getId();
			if (seenTeamIds.contains(teamId)) continue;
			seenTeamIds.add(teamId);
			teamCount++;

			String g = r.getGroupName() == null ? "" : r.getGroupName().trim().toUpperCase();
			if (g.isBlank()) {
				allAssigned = false;
			}
		}

		boolean readyToAssign = tournament.getTeamLimit() != null && teamCount == tournament.getTeamLimit() && teamCount == 16;
		boolean groupingLocked = allAssigned && readyToAssign;

		List<Match> matchesForGroups = allMatches;
		if (groupingLocked) {
			boolean hasGroupMatches = matchesForGroups.stream().anyMatch(m -> m != null && m.getRoundName() != null && m.getRoundName().startsWith("Bảng "));
			if (!hasGroupMatches) {
				boolean generated = matchService.generateGroupStageMatchesIfMissing(tournament, registrations);
				if (generated) {
					matchesForGroups = updateMatchStatusesByTime(matchService.listByTournamentIdWithDetails(tournamentId));
				}
			}
		}

		Map<String, List<Match>> groupMatches = new HashMap<>();
		groupMatches.put("A", new ArrayList<>());
		groupMatches.put("B", new ArrayList<>());
		groupMatches.put("C", new ArrayList<>());
		groupMatches.put("D", new ArrayList<>());

		Map<String, Map<Long, Integer>> pointsByGroup = new HashMap<>();
		pointsByGroup.put("A", new HashMap<>());
		pointsByGroup.put("B", new HashMap<>());
		pointsByGroup.put("C", new HashMap<>());
		pointsByGroup.put("D", new HashMap<>());

		Map<String, Map<Long, Integer>> goalsForByGroup = new HashMap<>();
		goalsForByGroup.put("A", new HashMap<>());
		goalsForByGroup.put("B", new HashMap<>());
		goalsForByGroup.put("C", new HashMap<>());
		goalsForByGroup.put("D", new HashMap<>());

		Map<String, Map<Long, Integer>> goalsAgainstByGroup = new HashMap<>();
		goalsAgainstByGroup.put("A", new HashMap<>());
		goalsAgainstByGroup.put("B", new HashMap<>());
		goalsAgainstByGroup.put("C", new HashMap<>());
		goalsAgainstByGroup.put("D", new HashMap<>());

		for (Match m : matchesForGroups) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim();
			String g = null;
			if ("Bảng A".equalsIgnoreCase(rn)) g = "A";
			if ("Bảng B".equalsIgnoreCase(rn)) g = "B";
			if ("Bảng C".equalsIgnoreCase(rn)) g = "C";
			if ("Bảng D".equalsIgnoreCase(rn)) g = "D";
			if (g == null) continue;
			groupMatches.get(g).add(m);

			Integer hs = m.getHomeScore();
			Integer as = m.getAwayScore();
			if (hs == null || as == null) continue;
			if (m.getHomeTeam() == null || m.getAwayTeam() == null) continue;
			Long hid = m.getHomeTeam().getId();
			Long aid = m.getAwayTeam().getId();
			if (hid == null || aid == null) continue;

			goalsForByGroup.get(g).put(hid, goalsForByGroup.get(g).getOrDefault(hid, 0) + hs);
			goalsAgainstByGroup.get(g).put(hid, goalsAgainstByGroup.get(g).getOrDefault(hid, 0) + as);
			goalsForByGroup.get(g).put(aid, goalsForByGroup.get(g).getOrDefault(aid, 0) + as);
			goalsAgainstByGroup.get(g).put(aid, goalsAgainstByGroup.get(g).getOrDefault(aid, 0) + hs);

			if (hs > as) {
				pointsByGroup.get(g).put(hid, pointsByGroup.get(g).getOrDefault(hid, 0) + 3);
			} else if (as > hs) {
				pointsByGroup.get(g).put(aid, pointsByGroup.get(g).getOrDefault(aid, 0) + 3);
			} else {
				pointsByGroup.get(g).put(hid, pointsByGroup.get(g).getOrDefault(hid, 0) + 1);
				pointsByGroup.get(g).put(aid, pointsByGroup.get(g).getOrDefault(aid, 0) + 1);
			}
		}

		for (TournamentRegistration r : registrations) {
			if (r == null) continue;
			if (r.getStatus() != RegistrationStatus.APPROVED) continue;
			if (r.getTeam() == null || r.getTeam().getId() == null) continue;
			Long teamId = r.getTeam().getId();
			if (!seenTeamIds.contains(teamId)) continue;
			String g = r.getGroupName() == null ? "" : r.getGroupName().trim().toUpperCase();
			if (!groupTeams.containsKey(g)) continue;
			long memberCount = playerRepository.countByTeamId(teamId);
			int points = pointsByGroup.get(g).getOrDefault(teamId, 0);
			int gf = goalsForByGroup.get(g).getOrDefault(teamId, 0);
			int ga = goalsAgainstByGroup.get(g).getOrDefault(teamId, 0);
			groupTeams.get(g).add(new GroupTeamRow(teamId, r.getTeam().getName(), memberCount, points, gf, ga));
		}

		for (String g : List.of("A", "B", "C", "D")) {
			groupTeams.get(g).sort(
					Comparator.comparingInt(GroupTeamRow::getPoints).reversed()
							.thenComparingInt(GroupTeamRow::getGoalDiff).reversed()
							.thenComparingInt(GroupTeamRow::getGoalsFor).reversed()
							.thenComparing(GroupTeamRow::getName, String.CASE_INSENSITIVE_ORDER)
			);
		}

		model.put("groupTeams", groupTeams);
		model.put("groupMatches", groupMatches);
		model.put("groupingLocked", groupingLocked);
		model.put("groupingReady", readyToAssign);

		if (groupingLocked) {
			boolean allGroupMatchesFinished = true;
			boolean hasAnyGroupMatch = false;
			for (List<Match> ms : groupMatches.values()) {
				for (Match m : ms) {
					hasAnyGroupMatch = true;
					if (m == null || m.getStatus() != MatchStatus.FINISHED) {
						allGroupMatchesFinished = false;
						break;
					}
				}
				if (!allGroupMatchesFinished) break;
			}

			boolean knockoutExists = knockoutOnly.stream().anyMatch(m -> {
				if (m == null || m.getRoundName() == null) return false;
				String rn = m.getRoundName().trim();
				return "Tứ kết".equalsIgnoreCase(rn) || "Bán kết".equalsIgnoreCase(rn) || "Chung kết".equalsIgnoreCase(rn);
			});

			if (hasAnyGroupMatch && allGroupMatchesFinished && !knockoutExists) {
				boolean created = matchService.generateQuarterFinalsFromGroupsIfReady(tournamentId);
				if (created) {
					List<Match> refreshed = updateMatchStatusesByTime(matchService.listByTournamentIdWithDetails(tournamentId));
					List<Match> refreshedKnockout = new ArrayList<>();
					for (Match m : refreshed) {
						if (m == null || m.getRoundName() == null) continue;
						String rn = m.getRoundName().trim().toLowerCase();
						if (rn.startsWith("bảng")) continue;
						refreshedKnockout.add(m);
					}
					PagedResult<Match> p2 = paginate(refreshedKnockout, page, size);
					model.put("knockoutMatches", p2.getItems());
					model.put("currentPage", p2.getCurrentPage());
					model.put("pageSize", p2.getPageSize());
					model.put("totalPages", p2.getTotalPages());
				}
			}
		}
	}

	private void applySelectedMatchModel(Map<String, Object> model, Long tournamentId, Long matchId, String tab, Tournament tournament) {
		Match selectedMatch = matchService.findByIdWithDetails(matchId).orElse(null);
		if (selectedMatch == null || selectedMatch.getTournament() == null || selectedMatch.getTournament().getId() == null
				|| !tournamentId.equals(selectedMatch.getTournament().getId())) {
			return;
		}

		Long homeTeamId = selectedMatch.getHomeTeam() == null ? null : selectedMatch.getHomeTeam().getId();
		Long awayTeamId = selectedMatch.getAwayTeam() == null ? null : selectedMatch.getAwayTeam().getId();

		List<PlayerDto> homePlayers = new ArrayList<>();
		List<PlayerDto> awayPlayers = new ArrayList<>();

		if (homeTeamId != null) {
			for (Player p : playerRepository.findByTeamIdOrderByJerseyNumberAsc(homeTeamId)) {
				homePlayers.add(new PlayerDto(p.getId(), p.getFullName(), p.getJerseyNumber(), p.getPosition(), p.getAvatarUrl()));
			}
		}
		if (awayTeamId != null) {
			for (Player p : playerRepository.findByTeamIdOrderByJerseyNumberAsc(awayTeamId)) {
				awayPlayers.add(new PlayerDto(p.getId(), p.getFullName(), p.getJerseyNumber(), p.getPosition(), p.getAvatarUrl()));
			}
		}

		List<PitchSlot> pitchSlots = buildPitchSlots(tournament.getPitchType());

		Map<Integer, PlayerDto> homeLineupByIndex = new HashMap<>();
		Map<Integer, PlayerDto> awayLineupByIndex = new HashMap<>();
		Set<Long> assignedHomePlayerIds = new HashSet<>();
		Set<Long> assignedAwayPlayerIds = new HashSet<>();

		for (MatchLineupSlot slot : matchLineupService.listByMatchId(matchId)) {
			if (slot == null || slot.getTeamSide() == null || slot.getSlotIndex() == null || slot.getPlayer() == null || slot.getPlayer().getId() == null) {
				continue;
			}
			Player p = slot.getPlayer();
			PlayerDto dto = new PlayerDto(p.getId(), p.getFullName(), p.getJerseyNumber(), p.getPosition(), p.getAvatarUrl());
			if (slot.getTeamSide() == TeamSide.HOME) {
				homeLineupByIndex.put(slot.getSlotIndex(), dto);
				assignedHomePlayerIds.add(p.getId());
			}
			if (slot.getTeamSide() == TeamSide.AWAY) {
				awayLineupByIndex.put(slot.getSlotIndex(), dto);
				assignedAwayPlayerIds.add(p.getId());
			}
		}

		List<MatchEventRow> eventRows = new ArrayList<>();
		for (MatchEvent ev : matchEventService.listByMatchId(matchId)) {
			if (ev == null || ev.getType() == null || ev.getMinute() == null || ev.getTeamSide() == null) continue;
			String teamName = ev.getTeamSide() == TeamSide.AWAY
					? (selectedMatch.getAwayTeam() != null ? selectedMatch.getAwayTeam().getName() : "Đội 2")
					: (selectedMatch.getHomeTeam() != null ? selectedMatch.getHomeTeam().getName() : "Đội 1");
			String playerName;
			if (ev.getPlayer() == null) {
				playerName = "N/A";
			} else {
				Integer jersey = ev.getPlayer().getJerseyNumber();
				String num = jersey == null ? "" : ("#" + jersey + " ");
				playerName = (num + (ev.getPlayer().getFullName() == null ? "" : ev.getPlayer().getFullName())).trim();
				if (playerName.isBlank()) playerName = "N/A";
			}
			eventRows.add(new MatchEventRow(ev.getId(), ev.getMinute(), teamName, playerName, ev.getType()));
		}
		eventRows.sort(Comparator.comparing(MatchEventRow::minute).thenComparing(MatchEventRow::id));

		model.put("selectedMatch", selectedMatch);
		model.put("selectedTab", tab == null ? "" : tab);
		model.put("pitchSlots", pitchSlots);
		model.put("selectedHomePlayers", homePlayers);
		model.put("selectedAwayPlayers", awayPlayers);
		model.put("homeLineupByIndex", homeLineupByIndex);
		model.put("awayLineupByIndex", awayLineupByIndex);
		model.put("assignedHomePlayerIds", assignedHomePlayerIds);
		model.put("assignedAwayPlayerIds", assignedAwayPlayerIds);
		model.put("matchEventRows", eventRows);
	}

	private List<Match> updateMatchStatusesByTime(List<Match> matches) {
		if (matches == null || matches.isEmpty()) return matches;
		LocalDateTime now = LocalDateTime.now();
		List<Match> toUpdate = new ArrayList<>();
		for (Match m : matches) {
			if (m == null) continue;
			if (m.getStatus() == null) {
				m.setStatus(MatchStatus.SCHEDULED);
				toUpdate.add(m);
				continue;
			}
			if (m.getStatus() == MatchStatus.FINISHED) continue;
			if (m.getScheduledAt() == null) continue;
			if (!m.getScheduledAt().isAfter(now) && m.getStatus() != MatchStatus.LIVE) {
				m.setStatus(MatchStatus.LIVE);
				toUpdate.add(m);
			}
		}
		if (!toUpdate.isEmpty()) {
			matchService.saveAll(toUpdate);
		}
		return matches;
	}

	private String firstRoundName(int teamLimit) {
		if (teamLimit == 4) return "Bán kết";
		if (teamLimit == 8) return "Tứ kết";
		if (teamLimit == 16) return "Vòng 16";
		return "Vòng loại";
	}

	private static <T> PagedResult<T> paginate(List<T> items, int page, int size) {
		int safeSize = Math.max(1, size);
		int totalItems = items == null ? 0 : items.size();
		int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
		int safePage = Math.min(Math.max(1, page), totalPages);
		int fromIndex = Math.min((safePage - 1) * safeSize, totalItems);
		int toIndex = Math.min(fromIndex + safeSize, totalItems);
		List<T> pageItems = totalItems == 0 ? List.of() : items.subList(fromIndex, toIndex);
		return new PagedResult<>(pageItems, safePage, safeSize, totalPages);
	}

	private static List<PitchSlot> buildPitchSlots(PitchType pitchType) {
		String normalized = pitchType == null ? "" : pitchType.name();
		if ("PITCH_5".equalsIgnoreCase(normalized)) {
			return List.of(
					new PitchSlot(0, 15, 30, "FW"),
					new PitchSlot(1, 15, 70, "FW"),
					new PitchSlot(2, 60, 30, "DF"),
					new PitchSlot(3, 60, 70, "DF"),
					new PitchSlot(4, 82, 50, "GK")
			);
		}
		return List.of(
				new PitchSlot(0, 12, 25, "FW"),
				new PitchSlot(1, 12, 50, "FW"),
				new PitchSlot(2, 12, 75, "FW"),
				new PitchSlot(3, 58, 25, "DF"),
				new PitchSlot(4, 58, 50, "DF"),
				new PitchSlot(5, 58, 75, "DF"),
				new PitchSlot(6, 82, 50, "GK")
		);
	}

	private static Long parseLongOrNull(String raw) {
		if (raw == null) return null;
		String s = raw.trim();
		if (s.isBlank()) return null;
		try {
			return Long.parseLong(s);
		} catch (Exception ignored) {
			return null;
		}
	}

	private void applyTournamentContext(Map<String, Object> model, Tournament tournament) {
		model.put("tournamentId", tournament.getId());
		model.put("tournamentName", tournament.getName());
	}

	public record PageResult(String viewName, Map<String, Object> model) {
		public static PageResult view(String viewName, Map<String, Object> model) {
			return new PageResult(viewName, model);
		}

		public static PageResult redirect(String to) {
			return new PageResult("redirect:" + to, Map.of());
		}
	}

	public static final class PagedResult<T> {
		private final List<T> items;
		private final int currentPage;
		private final int pageSize;
		private final int totalPages;

		public PagedResult(List<T> items, int currentPage, int pageSize, int totalPages) {
			this.items = items;
			this.currentPage = currentPage;
			this.pageSize = pageSize;
			this.totalPages = totalPages;
		}

		public List<T> getItems() { return items; }
		public int getCurrentPage() { return currentPage; }
		public int getPageSize() { return pageSize; }
		public int getTotalPages() { return totalPages; }
	}

	public record PlayerDto(Long id, String fullName, Integer jerseyNumber, String position, String avatarUrl) {
	}

	public record MatchPlayersResponse(List<PlayerDto> homePlayers, List<PlayerDto> awayPlayers, String lineupJson, String eventsJson) {
	}

	public record PitchSlot(int index, int top, int left, String label) {
	}

	public record MatchEventRow(Long id, Integer minute, String teamName, String playerName, MatchEventType type) {
	}

	public static final class GroupTeamRow {
		private final Long id;
		private final String name;
		private final long memberCount;
		private final int points;
		private final int goalsFor;
		private final int goalsAgainst;

		public GroupTeamRow(Long id, String name, long memberCount, int points, int goalsFor, int goalsAgainst) {
			this.id = id;
			this.name = name;
			this.memberCount = memberCount;
			this.points = points;
			this.goalsFor = goalsFor;
			this.goalsAgainst = goalsAgainst;
		}

		public Long getId() { return id; }
		public String getName() { return name; }
		public long getMemberCount() { return memberCount; }
		public int getPoints() { return points; }
		public int getGoalsFor() { return goalsFor; }
		public int getGoalsAgainst() { return goalsAgainst; }

		public int getGoalDiff() { return goalsFor - goalsAgainst; }
	}
}
