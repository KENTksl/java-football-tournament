package com.example.football_tourament_web.service;

import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.PitchType;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TeamSide;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.repository.PlayerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserTournamentViewService {
	private final TournamentService tournamentService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final MatchService matchService;
	private final MatchLineupService matchLineupService;
	private final TeamService teamService;
	private final UserService userService;
	private final PlayerRepository playerRepository;

	public UserTournamentViewService(
			TournamentService tournamentService,
			TournamentRegistrationService tournamentRegistrationService,
			MatchService matchService,
			MatchLineupService matchLineupService,
			TeamService teamService,
			UserService userService,
			PlayerRepository playerRepository
	) {
		this.tournamentService = tournamentService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.matchService = matchService;
		this.matchLineupService = matchLineupService;
		this.teamService = teamService;
		this.userService = userService;
		this.playerRepository = playerRepository;
	}

	@Transactional(readOnly = true)
	public Tournament findTournamentOrNull(Long id) {
		if (id == null) {
			return null;
		}
		return tournamentService.findById(id).orElse(null);
	}

	@Transactional(readOnly = true)
	public long countRegisteredTeams(Long tournamentId) {
		return tournamentRegistrationService.countRegisteredTeams(tournamentId);
	}

	@Transactional(readOnly = true)
	public Map<Long, Long> buildRegisteredTeamCountMap(List<Tournament> tournaments) {
		Map<Long, Long> result = new HashMap<>();
		if (tournaments == null) {
			return result;
		}
		for (Tournament tournament : tournaments) {
			if (tournament == null || tournament.getId() == null) {
				continue;
			}
			result.put(tournament.getId(), tournamentRegistrationService.countRegisteredTeams(tournament.getId()));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public Map<Long, Integer> buildRegisteredTeamPercentMap(List<Tournament> tournaments) {
		Map<Long, Integer> result = new HashMap<>();
		if (tournaments == null) {
			return result;
		}
		for (Tournament tournament : tournaments) {
			if (tournament == null || tournament.getId() == null) {
				continue;
			}
			long count = tournamentRegistrationService.countRegisteredTeams(tournament.getId());
			int percent = 0;
			if (tournament.getTeamLimit() != null && tournament.getTeamLimit() > 0) {
				percent = (int) Math.min(100, (count * 100 / tournament.getTeamLimit()));
			}
			result.put(tournament.getId(), percent);
		}
		return result;
	}

	public boolean isTournamentFull(Tournament tournament) {
		if (tournament == null || tournament.getId() == null || tournament.getTeamLimit() == null) {
			return false;
		}
		return tournamentRegistrationService.countRegisteredTeams(tournament.getId()) >= tournament.getTeamLimit();
	}

	@Transactional(readOnly = true)
	public TeamPrefillResponse buildTeamPrefill(Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return TeamPrefillResponse.notFound("Vui lòng đăng nhập để dùng dữ liệu đội");
		}

		var teamOpt = teamService.listByCaptain(user.getId()).stream().findFirst();
		if (teamOpt.isEmpty()) {
			return TeamPrefillResponse.notFound("Bạn chưa có đội bóng để sử dụng");
		}

		var team = teamOpt.get();
		var players = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId()).stream()
				.map(player -> new PlayerPrefill(player.getFullName(), player.getJerseyNumber(), player.getAvatarUrl()))
				.toList();

		return TeamPrefillResponse.found(
				team.getId(),
				team.getName(),
				user.getFullName(),
				user.getEmail(),
				user.getPhone(),
				team.getLogoUrl(),
				players
		);
	}

	@Transactional(readOnly = true)
	public List<SignUpTeamOption> listSignUpTeamOptions(Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return List.of();
		}
		return teamService.listByCaptain(user.getId()).stream()
				.filter(team -> team != null && team.getId() != null)
				.map(team -> new SignUpTeamOption(team.getId(), team.getName()))
				.toList();
	}

	@Transactional
	public RegistrationSubmitResult submitRegistration(Authentication authentication, Long tournamentId, Long teamId) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return RegistrationSubmitResult.failed("Vui lòng đăng nhập để đăng ký");
		}
		if (tournamentId == null) {
			return RegistrationSubmitResult.failed("Thiếu thông tin giải đấu");
		}
		if (teamId == null) {
			return RegistrationSubmitResult.failed("Vui lòng chọn đội bóng để đăng ký");
		}

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return RegistrationSubmitResult.failed("Không tìm thấy giải đấu");
		}
		var teamOpt = teamService.findById(teamId);
		if (teamOpt.isEmpty()) {
			return RegistrationSubmitResult.failed("Không tìm thấy đội bóng");
		}
		var team = teamOpt.get();
		if (team.getCaptain() == null || team.getCaptain().getId() == null || !team.getCaptain().getId().equals(user.getId())) {
			return RegistrationSubmitResult.failed("Bạn không có quyền đăng ký đội bóng này");
		}

		var existing = tournamentRegistrationService.findByTournamentAndTeam(tournamentId, teamId).orElse(null);
		if (existing != null && existing.getStatus() == RegistrationStatus.APPROVED) {
			return RegistrationSubmitResult.failed("Đội đã được duyệt trong giải đấu này");
		}
		if (existing != null && existing.getStatus() == RegistrationStatus.PENDING) {
			return RegistrationSubmitResult.failed("Đội đã gửi hồ sơ và đang chờ duyệt");
		}
		long registeredTeams = tournamentRegistrationService.countRegisteredTeams(tournamentId);
		if (tournament.getTeamLimit() != null && registeredTeams >= tournament.getTeamLimit()) {
			return RegistrationSubmitResult.failed("Giải đấu đã đủ số lượng đội tham gia");
		}
		if (existing == null) {
			existing = new TournamentRegistration();
			existing.setTournament(tournament);
			existing.setTeam(team);
		}
		existing.setRegisteredBy(user);
		existing.setStatus(RegistrationStatus.PENDING);
		existing.setGroupName(null);
		tournamentRegistrationService.save(existing);
		return RegistrationSubmitResult.success("Đã gửi hồ sơ đăng ký. Admin sẽ duyệt trong thời gian sớm nhất");
	}

	@Transactional(readOnly = true)
	public List<TeamCardView> buildTournamentTeams(Long tournamentId) {
		if (tournamentId == null) {
			return List.of();
		}
		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<TeamCardView> teams = new ArrayList<>();
		Set<Long> seenTeamIds = new HashSet<>();
		for (TournamentRegistration registration : registrations) {
			if (registration == null || registration.getStatus() != RegistrationStatus.APPROVED) {
				continue;
			}
			if (registration.getTeam() == null || registration.getTeam().getId() == null) {
				continue;
			}
			Long teamId = registration.getTeam().getId();
			if (seenTeamIds.contains(teamId)) {
				continue;
			}
			seenTeamIds.add(teamId);
			String teamName = registration.getTeam().getName();
			String captainName = registration.getTeam().getCaptain() == null ? null : registration.getTeam().getCaptain().getFullName();
			long memberCount = playerRepository.countByTeamId(teamId);
			String teamCode = "TU" + teamId;
			String coverLetter = (teamName == null || teamName.isBlank()) ? "T" : String.valueOf(Character.toUpperCase(teamName.trim().charAt(0)));
			teams.add(new TeamCardView(
					teamId,
					teamName,
					captainName,
					teamCode,
					memberCount,
					coverLetter,
					registration.getTeam().getLogoUrl()
			));
		}
		teams.sort(Comparator.comparing(TeamCardView::teamName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
		return teams;
	}

	@Transactional(readOnly = true)
	public MatchLineupView buildMatchLineupView(Long tournamentId, Long matchId) {
		if (tournamentId == null || matchId == null) {
			return MatchLineupView.notFound("Thiếu thông tin trận đấu");
		}
		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return MatchLineupView.notFound("Không tìm thấy dữ liệu đội hình");
		}
		if (match.getHomeTeam() == null || match.getAwayTeam() == null
				|| match.getHomeTeam().getId() == null || match.getAwayTeam().getId() == null) {
			return MatchLineupView.notFound("Trận đấu chưa có đủ thông tin 2 đội");
		}

		List<LineupSlotView> pitchSlots = buildPitchSlots(match.getTournament().getPitchType());
		Map<Integer, LineupSlotPlayerView> homeAssigned = new HashMap<>();
		Map<Integer, LineupSlotPlayerView> awayAssigned = new HashMap<>();
		Set<Long> homeAssignedPlayerIds = new HashSet<>();
		Set<Long> awayAssignedPlayerIds = new HashSet<>();

		for (var slot : matchLineupService.listByMatchId(matchId)) {
			if (slot == null || slot.getPlayer() == null || slot.getPlayer().getId() == null || slot.getSlotIndex() == null) {
				continue;
			}
			PlayerLineupView playerView = new PlayerLineupView(
					slot.getPlayer().getId(),
					slot.getPlayer().getFullName(),
					slot.getPlayer().getJerseyNumber(),
					slot.getPlayer().getPosition(),
					slot.getPlayer().getAvatarUrl()
			);
			LineupSlotPlayerView slotPlayerView = new LineupSlotPlayerView(slot.getSlotIndex(), slot.getPosition(), playerView);
			if (slot.getTeamSide() == TeamSide.HOME) {
				homeAssigned.put(slot.getSlotIndex(), slotPlayerView);
				homeAssignedPlayerIds.add(slot.getPlayer().getId());
			}
			if (slot.getTeamSide() == TeamSide.AWAY) {
				awayAssigned.put(slot.getSlotIndex(), slotPlayerView);
				awayAssignedPlayerIds.add(slot.getPlayer().getId());
			}
		}

		List<PlayerLineupView> homeBench = new ArrayList<>();
		for (var player : playerRepository.findByTeamIdOrderByJerseyNumberAsc(match.getHomeTeam().getId())) {
			if (player == null || player.getId() == null || homeAssignedPlayerIds.contains(player.getId())) {
				continue;
			}
			homeBench.add(new PlayerLineupView(player.getId(), player.getFullName(), player.getJerseyNumber(), player.getPosition(), player.getAvatarUrl()));
		}

		List<PlayerLineupView> awayBench = new ArrayList<>();
		for (var player : playerRepository.findByTeamIdOrderByJerseyNumberAsc(match.getAwayTeam().getId())) {
			if (player == null || player.getId() == null || awayAssignedPlayerIds.contains(player.getId())) {
				continue;
			}
			awayBench.add(new PlayerLineupView(player.getId(), player.getFullName(), player.getJerseyNumber(), player.getPosition(), player.getAvatarUrl()));
		}

		List<LineupSlotPlayerView> homeStarters = new ArrayList<>();
		List<LineupSlotPlayerView> awayStarters = new ArrayList<>();
		for (LineupSlotView slot : pitchSlots) {
			homeStarters.add(homeAssigned.getOrDefault(slot.index(), new LineupSlotPlayerView(slot.index(), slot.label(), null)));
			awayStarters.add(awayAssigned.getOrDefault(slot.index(), new LineupSlotPlayerView(slot.index(), slot.label(), null)));
		}

		TeamLineupView homeTeam = new TeamLineupView(match.getHomeTeam().getName(), homeStarters, homeBench);
		TeamLineupView awayTeam = new TeamLineupView(match.getAwayTeam().getName(), awayStarters, awayBench);

		boolean hasLineup = homeAssignedPlayerIds.size() + awayAssignedPlayerIds.size() > 0;
		String message = hasLineup ? "" : "Admin chưa cập nhật đội hình trận này";
		return new MatchLineupView(
				true,
				message,
				match.getRoundName(),
				match.getScheduledAt(),
				displayMatchStatus(match.getStatus()),
				match.getHomeScore(),
				match.getAwayScore(),
				pitchSlots,
				homeTeam,
				awayTeam
		);
	}

	@Transactional(readOnly = true)
	public TeamSingleLineupView buildTeamLineupView(Long tournamentId, Long teamId) {
		if (tournamentId == null || teamId == null) {
			return TeamSingleLineupView.notFound("Thiếu thông tin đội bóng");
		}
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return TeamSingleLineupView.notFound("Không tìm thấy giải đấu");
		}
		var teamOpt = teamService.findById(teamId);
		if (teamOpt.isEmpty()) {
			return TeamSingleLineupView.notFound("Không tìm thấy đội bóng");
		}
		var team = teamOpt.get();

		boolean belongsToTournament = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId).stream()
				.anyMatch(r -> r != null && r.getTeam() != null && teamId.equals(r.getTeam().getId()));
		if (!belongsToTournament) {
			return TeamSingleLineupView.notFound("Đội bóng không thuộc giải đấu này");
		}

		List<LineupSlotView> pitchSlots = buildPitchSlots(tournament.getPitchType());
		List<LineupSlotPlayerView> starters = List.of();

		List<Match> matches = matchService.listByTournamentIdWithDetails(tournamentId).stream()
				.sorted(Comparator
						.comparing((Match m) -> m.getScheduledAt() == null ? LocalDateTime.MIN : m.getScheduledAt())
						.reversed()
						.thenComparing(m -> m.getId() == null ? Long.MIN_VALUE : m.getId(), Comparator.reverseOrder()))
				.toList();

		for (Match match : matches) {
			if (match == null || match.getId() == null) {
				continue;
			}
			List<LineupSlotPlayerView> candidate = matchLineupService.listByMatchId(match.getId()).stream()
					.filter(slot -> slot != null && slot.getPlayer() != null && slot.getPlayer().getTeam() != null
							&& slot.getPlayer().getTeam().getId() != null && teamId.equals(slot.getPlayer().getTeam().getId()))
					.sorted(Comparator.comparing(slot -> slot.getSlotIndex() == null ? Integer.MAX_VALUE : slot.getSlotIndex()))
					.map(slot -> new LineupSlotPlayerView(
							slot.getSlotIndex() == null ? -1 : slot.getSlotIndex(),
							slot.getPosition(),
							new PlayerLineupView(
									slot.getPlayer().getId(),
									slot.getPlayer().getFullName(),
									slot.getPlayer().getJerseyNumber(),
									slot.getPlayer().getPosition(),
									slot.getPlayer().getAvatarUrl()
							)
					))
					.toList();
			if (!candidate.isEmpty()) {
				starters = candidate;
				break;
			}
		}

		Set<Long> starterIds = starters.stream()
				.map(LineupSlotPlayerView::player)
				.map(player -> player == null ? null : player.id())
				.filter(id -> id != null)
				.collect(java.util.stream.Collectors.toSet());

		List<PlayerLineupView> bench = playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId).stream()
				.filter(player -> player != null && player.getId() != null && !starterIds.contains(player.getId()))
				.map(player -> new PlayerLineupView(
						player.getId(),
						player.getFullName(),
						player.getJerseyNumber(),
						player.getPosition(),
						player.getAvatarUrl()
				))
				.toList();

		boolean hasLineup = !starters.isEmpty();
		String message = hasLineup ? "" : "Đội chưa có đội hình được admin thiết lập";
		return new TeamSingleLineupView(true, message, team.getName(), pitchSlots, starters, bench);
	}

	@Transactional(readOnly = true)
	public ScheduleView buildScheduleView(Long tournamentId) {
		if (tournamentId == null) {
			return ScheduleView.none("Chọn giải đấu để xem lịch thi đấu");
		}

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return ScheduleView.none("Không tìm thấy giải đấu");
		}

		List<Match> allMatches = matchService.listByTournamentIdWithDetails(tournamentId);
		List<Match> groupMatchesSource = new ArrayList<>();
		List<MatchRow> knockoutMatches = new ArrayList<>();

		for (var match : allMatches) {
			if (match == null || match.getRoundName() == null) {
				continue;
			}
			String roundName = match.getRoundName().trim();
			if (roundName.startsWith("Bảng ")) {
				groupMatchesSource.add(match);
			} else {
				knockoutMatches.add(toMatchRow(match));
			}
		}
		knockoutMatches.sort(Comparator.comparing(m -> m.scheduledAt() == null ? LocalDateTime.MAX : m.scheduledAt()));

		if (tournament.getMode() == TournamentMode.GROUP_STAGE) {
			Map<String, List<GroupTeamRow>> groupTeams = new HashMap<>();
			Map<String, List<MatchRow>> groupMatches = new HashMap<>();
			Map<String, Map<Long, TeamStats>> statsByGroup = new HashMap<>();

			for (String group : List.of("A", "B", "C", "D")) {
				groupTeams.put(group, new ArrayList<>());
				groupMatches.put(group, new ArrayList<>());
				statsByGroup.put(group, new HashMap<>());
			}

			List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
			List<Long> seenTeamIds = new ArrayList<>();
			int validTeamCount = 0;
			boolean allAssigned = true;

			for (TournamentRegistration registration : registrations) {
				if (registration == null || registration.getStatus() == RegistrationStatus.REJECTED) {
					continue;
				}
				if (registration.getTeam() == null || registration.getTeam().getId() == null) {
					continue;
				}
				Long teamId = registration.getTeam().getId();
				if (seenTeamIds.contains(teamId)) {
					continue;
				}
				seenTeamIds.add(teamId);
				validTeamCount++;

				String groupName = registration.getGroupName() == null ? "" : registration.getGroupName().trim().toUpperCase();
				if (!groupTeams.containsKey(groupName)) {
					allAssigned = false;
					continue;
				}
				statsByGroup.get(groupName).put(teamId, new TeamStats());
				long memberCount = playerRepository.countByTeamId(teamId);
				groupTeams.get(groupName).add(new GroupTeamRow(teamId, registration.getTeam().getName(), memberCount, 0, 0, 0));
			}

			boolean groupingReady = tournament.getTeamLimit() != null && validTeamCount == tournament.getTeamLimit() && validTeamCount == 16;
			boolean groupingLocked = groupingReady && allAssigned;
			boolean hasGroupMatches = !groupMatchesSource.isEmpty();
			boolean scheduleReady = hasGroupMatches;

			for (var match : groupMatchesSource) {
				String groupName = toGroupName(match.getRoundName());
				if (!groupMatches.containsKey(groupName)) {
					continue;
				}

				if (match.getHomeTeam() != null && match.getHomeTeam().getId() != null) {
					ensureTeamInGroup(groupTeams, statsByGroup, groupName, match.getHomeTeam().getId(), match.getHomeTeam().getName());
				}
				if (match.getAwayTeam() != null && match.getAwayTeam().getId() != null) {
					ensureTeamInGroup(groupTeams, statsByGroup, groupName, match.getAwayTeam().getId(), match.getAwayTeam().getName());
				}

				groupMatches.get(groupName).add(toMatchRow(match));

				if (match.getStatus() != MatchStatus.FINISHED || match.getHomeScore() == null || match.getAwayScore() == null) {
					continue;
				}
				if (match.getHomeTeam() == null || match.getAwayTeam() == null || match.getHomeTeam().getId() == null || match.getAwayTeam().getId() == null) {
					continue;
				}
				Map<Long, TeamStats> groupStats = statsByGroup.get(groupName);
				if (groupStats == null) {
					continue;
				}
				TeamStats homeStats = groupStats.get(match.getHomeTeam().getId());
				TeamStats awayStats = groupStats.get(match.getAwayTeam().getId());
				if (homeStats == null || awayStats == null) {
					continue;
				}
				int homeScore = match.getHomeScore();
				int awayScore = match.getAwayScore();
				homeStats.goalsFor += homeScore;
				homeStats.goalsAgainst += awayScore;
				awayStats.goalsFor += awayScore;
				awayStats.goalsAgainst += homeScore;
				if (homeScore > awayScore) {
					homeStats.points += 3;
				} else if (awayScore > homeScore) {
					awayStats.points += 3;
				} else {
					homeStats.points += 1;
					awayStats.points += 1;
				}
			}

			for (String group : List.of("A", "B", "C", "D")) {
				List<GroupTeamRow> rows = new ArrayList<>();
				for (GroupTeamRow row : groupTeams.get(group)) {
					TeamStats stats = statsByGroup.get(group).getOrDefault(row.teamId(), new TeamStats());
					rows.add(new GroupTeamRow(row.teamId(), row.name(), row.memberCount(), stats.points, stats.goalsFor, stats.goalsAgainst));
				}
				rows.sort(
						Comparator.comparingInt(GroupTeamRow::points).reversed()
								.thenComparingInt(GroupTeamRow::goalDiff).reversed()
								.thenComparingInt(GroupTeamRow::goalsFor).reversed()
								.thenComparing(GroupTeamRow::name, String.CASE_INSENSITIVE_ORDER)
				);
				groupTeams.put(group, rows);
				groupMatches.get(group).sort(Comparator.comparing(m -> m.scheduledAt() == null ? LocalDateTime.MAX : m.scheduledAt()));
			}

			String message = scheduleReady ? "" : "Giải đấu chưa chia bảng nên chưa có lịch thi đấu";
			return new ScheduleView(scheduleReady, "GROUP_STAGE", message, groupingLocked, groupTeams, groupMatches, knockoutMatches);
		}

		boolean scheduleReady = !knockoutMatches.isEmpty();
		String message = scheduleReady ? "" : "Giải đấu chưa chia cặp nên chưa có lịch thi đấu";
		return new ScheduleView(scheduleReady, "KNOCKOUT", message, false, Map.of(), Map.of(), knockoutMatches);
	}

	private String toGroupName(String roundName) {
		String value = roundName == null ? "" : roundName.trim();
		if ("Bảng A".equalsIgnoreCase(value)) return "A";
		if ("Bảng B".equalsIgnoreCase(value)) return "B";
		if ("Bảng C".equalsIgnoreCase(value)) return "C";
		if ("Bảng D".equalsIgnoreCase(value)) return "D";
		return "";
	}

	private List<LineupSlotView> buildPitchSlots(PitchType pitchType) {
		String normalized = pitchType == null ? "" : pitchType.name();
		if ("PITCH_5".equalsIgnoreCase(normalized)) {
			return List.of(
					new LineupSlotView(0, 15, 30, "FW"),
					new LineupSlotView(1, 15, 70, "FW"),
					new LineupSlotView(2, 60, 30, "DF"),
					new LineupSlotView(3, 60, 70, "DF"),
					new LineupSlotView(4, 82, 50, "GK")
			);
		}
		return List.of(
				new LineupSlotView(0, 12, 25, "FW"),
				new LineupSlotView(1, 12, 50, "FW"),
				new LineupSlotView(2, 12, 75, "FW"),
				new LineupSlotView(3, 58, 25, "DF"),
				new LineupSlotView(4, 58, 50, "DF"),
				new LineupSlotView(5, 58, 75, "DF"),
				new LineupSlotView(6, 82, 50, "GK")
		);
	}

	private MatchRow toMatchRow(Match match) {
		return new MatchRow(
				match.getId(),
				match.getRoundName(),
				match.getScheduledAt(),
				match.getHomeTeam() == null ? "—" : match.getHomeTeam().getName(),
				match.getAwayTeam() == null ? "—" : match.getAwayTeam().getName(),
				match.getHomeScore(),
				match.getAwayScore(),
				displayMatchStatus(match.getStatus())
		);
	}

	private String displayMatchStatus(MatchStatus status) {
		if (status == null) return "Đang cập nhật";
		return switch (status) {
			case FINISHED -> "Hoàn thành";
			case LIVE -> "Đang diễn ra";
			case SCHEDULED -> "Sắp diễn ra";
		};
	}

	private void ensureTeamInGroup(
			Map<String, List<GroupTeamRow>> groupTeams,
			Map<String, Map<Long, TeamStats>> statsByGroup,
			String groupName,
			Long teamId,
			String teamName
	) {
		if (groupName == null || !groupTeams.containsKey(groupName) || teamId == null) {
			return;
		}
		List<GroupTeamRow> rows = groupTeams.get(groupName);
		boolean exists = rows.stream().anyMatch(row -> row != null && teamId.equals(row.teamId()));
		if (!exists) {
			long memberCount = playerRepository.countByTeamId(teamId);
			rows.add(new GroupTeamRow(teamId, teamName, memberCount, 0, 0, 0));
		}
		statsByGroup.computeIfAbsent(groupName, key -> new HashMap<>()).putIfAbsent(teamId, new TeamStats());
	}

	private com.example.football_tourament_web.model.entity.AppUser requireCurrentUser(Authentication authentication) {
		String email = authentication == null ? null : authentication.getName();
		if (email == null || email.isBlank()) {
			return null;
		}
		return userService.findByEmail(email).orElse(null);
	}

	private static final class TeamStats {
		private int points;
		private int goalsFor;
		private int goalsAgainst;
	}

	public record PlayerPrefill(String fullName, Integer jerseyNumber, String avatarUrl) {
	}

	public record TeamPrefillResponse(
			boolean found,
			String message,
			Long teamId,
			String teamName,
			String representativeName,
			String contactEmail,
			String contactPhone,
			String logoUrl,
			List<PlayerPrefill> players
	) {
		public static TeamPrefillResponse found(
				Long teamId,
				String teamName,
				String representativeName,
				String contactEmail,
				String contactPhone,
				String logoUrl,
				List<PlayerPrefill> players
		) {
			return new TeamPrefillResponse(
					true,
					null,
					teamId,
					teamName,
					representativeName,
					contactEmail,
					contactPhone,
					logoUrl,
					players
			);
		}

		public static TeamPrefillResponse notFound(String message) {
			return new TeamPrefillResponse(false, message, null, null, null, null, null, null, List.of());
		}
	}

	public record SignUpTeamOption(Long id, String name) {
	}

	public record RegistrationSubmitResult(boolean success, String message) {
		public static RegistrationSubmitResult success(String message) {
			return new RegistrationSubmitResult(true, message);
		}
		public static RegistrationSubmitResult failed(String message) {
			return new RegistrationSubmitResult(false, message);
		}
	}

	public record GroupTeamRow(Long teamId, String name, long memberCount, int points, int goalsFor, int goalsAgainst) {
		public int goalDiff() {
			return goalsFor - goalsAgainst;
		}
	}

	public record MatchRow(
			Long id,
			String roundName,
			LocalDateTime scheduledAt,
			String homeTeamName,
			String awayTeamName,
			Integer homeScore,
			Integer awayScore,
			String statusLabel
	) {
	}

	public record ScheduleView(
			boolean scheduleReady,
			String scheduleMode,
			String scheduleMessage,
			boolean groupingLocked,
			Map<String, List<GroupTeamRow>> groupTeams,
			Map<String, List<MatchRow>> groupMatches,
			List<MatchRow> knockoutMatches
	) {
		public static ScheduleView none(String message) {
			return new ScheduleView(false, "NONE", message, false, Map.of(), Map.of(), List.of());
		}
	}

	public record LineupSlotView(int index, int top, int left, String label) {
	}

	public record PlayerLineupView(
			Long id,
			String fullName,
			Integer jerseyNumber,
			String position,
			String avatarUrl
	) {
	}

	public record LineupSlotPlayerView(
			int slotIndex,
			String position,
			PlayerLineupView player
	) {
	}

	public record TeamLineupView(
			String teamName,
			List<LineupSlotPlayerView> starters,
			List<PlayerLineupView> bench
	) {
	}

	public record MatchLineupView(
			boolean found,
			String message,
			String roundName,
			LocalDateTime scheduledAt,
			String statusLabel,
			Integer homeScore,
			Integer awayScore,
			List<LineupSlotView> pitchSlots,
			TeamLineupView homeTeam,
			TeamLineupView awayTeam
	) {
		public static MatchLineupView notFound(String message) {
			return new MatchLineupView(false, message, null, null, null, null, null, List.of(), null, null);
		}
	}

	public record TeamCardView(
			Long teamId,
			String teamName,
			String captainName,
			String teamCode,
			long memberCount,
			String coverLetter,
			String logoUrl
	) {
	}

	public record TeamSingleLineupView(
			boolean found,
			String message,
			String teamName,
			List<LineupSlotView> pitchSlots,
			List<LineupSlotPlayerView> starters,
			List<PlayerLineupView> bench
	) {
		public static TeamSingleLineupView notFound(String message) {
			return new TeamSingleLineupView(false, message, null, List.of(), List.of(), List.of());
		}
	}
}
