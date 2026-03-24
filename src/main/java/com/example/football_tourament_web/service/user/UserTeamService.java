package com.example.football_tourament_web.service.user;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.repository.MatchEventRepository;
import com.example.football_tourament_web.repository.MatchLineupSlotRepository;
import com.example.football_tourament_web.repository.MatchRepository;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.common.FileStorageService;
import com.example.football_tourament_web.service.common.ImageService;
import com.example.football_tourament_web.service.core.TeamService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.common.ViewFormatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserTeamService {
	private final UserProfileService userProfileService;
	private final TeamService teamService;
	private final PlayerRepository playerRepository;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final MatchRepository matchRepository;
	private final MatchEventRepository matchEventRepository;
	private final MatchLineupSlotRepository matchLineupSlotRepository;
	private final FileStorageService fileStorageService;
	private final ViewFormatService viewFormatService;
	private final ImageService imageService;

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

	public UserTeamService(
			UserProfileService userProfileService,
			TeamService teamService,
			PlayerRepository playerRepository,
			TournamentRegistrationService tournamentRegistrationService,
			MatchRepository matchRepository,
			MatchEventRepository matchEventRepository,
			MatchLineupSlotRepository matchLineupSlotRepository,
			FileStorageService fileStorageService,
			ViewFormatService viewFormatService,
			ImageService imageService
	) {
		this.userProfileService = userProfileService;
		this.teamService = teamService;
		this.playerRepository = playerRepository;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.matchRepository = matchRepository;
		this.matchEventRepository = matchEventRepository;
		this.matchLineupSlotRepository = matchLineupSlotRepository;
		this.fileStorageService = fileStorageService;
		this.viewFormatService = viewFormatService;
		this.imageService = imageService;
	}

	public AppUser requireCurrentUser(Authentication authentication) {
		return userProfileService.requireCurrentUser(authentication);
	}

	public void attachCommonProfileModel(Model model, AppUser user) {
		userProfileService.attachCommonProfileModel(model, user);
	}

	public String buildTeamInfoPage(
			Long teamId,
			Long tournamentId,
			String tab,
			String create,
			String edit,
			Model model,
			Authentication authentication
	) {
		AppUser user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		attachCommonProfileModel(model, user);

		List<TeamCardView> teams = teamService.listByCaptain(user.getId()).stream()
				.map(t -> new TeamCardView(
						t.getId(),
						t.getName(),
						imageService.resolveTournamentCoverUrl(t.getLogoUrl()),
						(int) playerRepository.countByTeamId(t.getId())
				))
				.collect(Collectors.toList());
		model.addAttribute("teams", teams);

		boolean canCreateTeam = teams.size() < 2;
		model.addAttribute("canCreateTeam", canCreateTeam);

		Long selectedTeamId = teamId;
		if (selectedTeamId == null && !teams.isEmpty()) {
			selectedTeamId = teams.get(0).id();
		}

		boolean isCreate = create != null;
		boolean isEdit = edit != null;

		if (isCreate && !canCreateTeam) {
			return "redirect:/thong-tin-doi?limitReached";
		}

		if (selectedTeamId != null) {
			var selectedOpt = teamService.findById(selectedTeamId)
					.filter(t -> t.getCaptain() != null && t.getCaptain().getId().equals(user.getId()));
			if (selectedOpt.isPresent()) {
				Team selectedTeam = selectedOpt.get();
				List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(selectedTeam.getId());
				model.addAttribute("selectedTeam", selectedTeam);
				model.addAttribute("members", members);

				if (isEdit) {
					model.addAttribute("editTeamId", selectedTeam.getId());
					if (!model.containsAttribute("teamForm")) {
						TeamCreateForm f = new TeamCreateForm();
						f.setTeamName(selectedTeam.getName());
						model.addAttribute("teamForm", f);
					}
					ensureMemberSlots(model, members);
				}

				if (!isCreate && !isEdit) {
					TeamEditLock lock = buildTeamEditLock(selectedTeam.getId());
					model.addAttribute("teamEditLocked", lock.locked());
					model.addAttribute("teamEditLockMessage", lock.message());
					model.addAttribute("activeTeamTab", tab == null || tab.isBlank() ? "info" : tab);
					attachTeamDetailModel(model, selectedTeam, tournamentId);
				}
			}
		}

		model.addAttribute("isCreate", isCreate);
		model.addAttribute("isEdit", isEdit);
		if (isCreate && !model.containsAttribute("teamForm")) {
			model.addAttribute("teamForm", new TeamCreateForm());
		}
		if (isCreate) {
			ensureMemberSlots(model, List.of());
		}
		return "user/profile/team-info";
	}

	@Transactional
	public String createTeam(
			@Valid TeamCreateForm teamForm,
			BindingResult bindingResult,
			MultipartFile teamLogo,
			List<String> memberNames,
			List<Integer> memberJerseys,
			MultipartFile[] memberAvatars,
			Authentication authentication,
			Model model
	) {
		AppUser user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		if (teamService.countByCaptain(user.getId()) >= 2) {
			return "redirect:/thong-tin-doi?limitReached";
		}

		if (bindingResult.hasErrors()) {
			return renderCreateTeamError(model, user, teamForm, bindingResult);
		}

		String name = teamForm.teamName == null ? "" : teamForm.teamName.trim();
		if (name.isBlank()) {
			bindingResult.rejectValue("teamName", "teamName.blank", "Vui lòng nhập tên đội");
			return renderCreateTeamError(model, user, teamForm, bindingResult);
		}

		if (teamService.findByName(name).isPresent()) {
			bindingResult.rejectValue("teamName", "teamName.exists", "Đã có đội với tên này, vui lòng đặt tên khác");
			return renderCreateTeamError(model, user, teamForm, bindingResult);
		}

		String logoUrl = null;
		try {
			if (teamLogo != null && !teamLogo.isEmpty()) {
				logoUrl = fileStorageService.storeValidatedImageUnderUploads(teamLogo, "teams", 2L * 1024 * 1024);
			}
		} catch (FileStorageService.FileTooLargeException ex) {
			return "redirect:/thong-tin-doi?create=1&logoTooLarge";
		} catch (FileStorageService.InvalidFileTypeException ex) {
			return "redirect:/thong-tin-doi?create=1&logoInvalidType";
		} catch (Exception ex) {
			return "redirect:/thong-tin-doi?create=1&logoError";
		}

		Team team = new Team(name);
		team.setCaptain(user);
		team.setLogoUrl(logoUrl);
		team = teamService.save(team);

		List<Player> players = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String n = memberNames != null && i < memberNames.size() ? memberNames.get(i) : null;
			if (n == null || n.trim().isBlank()) {
				continue;
			}

			Integer jersey = memberJerseys != null && i < memberJerseys.size() ? memberJerseys.get(i) : null;
			if (jersey != null && (jersey < 0 || jersey > 999)) {
				jersey = null;
			}

			String avatarUrl = null;
			try {
				MultipartFile avatarFile = memberAvatars != null && i < memberAvatars.length ? memberAvatars[i] : null;
				if (avatarFile != null && !avatarFile.isEmpty()) {
					avatarUrl = fileStorageService.storeValidatedImageUnderUploads(avatarFile, "players", 2L * 1024 * 1024);
				}
			} catch (Exception ex) {
				avatarUrl = null;
			}

			Player p = new Player(n.trim(), team);
			p.setJerseyNumber(jersey);
			p.setRole("Cầu thủ");
			p.setAvatarUrl(avatarUrl);
			players.add(p);
		}
		if (!players.isEmpty()) {
			playerRepository.saveAll(players);
		}

		return "redirect:/thong-tin-doi?teamId=" + team.getId();
	}

	private TeamEditLock buildTeamEditLock(Long teamId) {
		if (teamId == null) return new TeamEditLock(false, "");
		List<Match> matches = matchRepository.findByTeamIdWithDetails(teamId);
		boolean hasNonFinished = matches.stream()
				.anyMatch(m -> m != null && m.getStatus() != null && m.getStatus() != MatchStatus.FINISHED);
		if (hasNonFinished) {
			return new TeamEditLock(true, "Đội đang có trận đấu (sắp diễn ra/đang diễn ra) nên không thể chỉnh sửa đội hình lúc này.");
		}
		boolean hasEventHistory = matchEventRepository.countByPlayerTeamId(teamId) > 0;
		boolean hasLineupHistory = matchLineupSlotRepository.countByPlayerTeamId(teamId) > 0;
		if (hasEventHistory || hasLineupHistory) {
			return new TeamEditLock(true, "Đội đã có lịch sử thi đấu nên không thể chỉnh sửa danh sách cầu thủ để đảm bảo dữ liệu trận đấu không bị sai.");
		}
		return new TeamEditLock(false, "");
	}

	@Transactional
	public String updateTeam(
			Long teamId,
			@Valid TeamCreateForm teamForm,
			BindingResult bindingResult,
			String existingLogoUrl,
			MultipartFile teamLogo,
			List<String> memberNames,
			List<Integer> memberJerseys,
			List<String> existingMemberAvatars,
			MultipartFile[] memberAvatars,
			Authentication authentication,
			Model model
	) {
		AppUser user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		var teamOpt = teamService.findById(teamId)
				.filter(t -> t.getCaptain() != null && t.getCaptain().getId().equals(user.getId()));
		if (teamOpt.isEmpty()) {
			return "redirect:/thong-tin-doi";
		}
		Team team = teamOpt.get();

		TeamEditLock lock = buildTeamEditLock(team.getId());
		if (lock.locked()) {
			return "redirect:/thong-tin-doi?teamId=" + team.getId() + "&editLocked=1";
		}

		if (bindingResult.hasErrors()) {
			return renderEditTeamError(model, user, team, teamForm, bindingResult);
		}

		String name = teamForm.teamName == null ? "" : teamForm.teamName.trim();
		if (name.isBlank()) {
			bindingResult.rejectValue("teamName", "teamName.blank", "Vui lòng nhập tên đội");
			return renderEditTeamError(model, user, team, teamForm, bindingResult);
		}

		var existingByName = teamService.findByName(name);
		if (existingByName.isPresent() && !existingByName.get().getId().equals(team.getId())) {
			bindingResult.rejectValue("teamName", "teamName.exists", "Đã có đội với tên này, vui lòng đặt tên khác");
			return renderEditTeamError(model, user, team, teamForm, bindingResult);
		}

		String nextLogoUrl = existingLogoUrl;
		try {
			if (teamLogo != null && !teamLogo.isEmpty()) {
				nextLogoUrl = fileStorageService.storeValidatedImageUnderUploads(teamLogo, "teams", 2L * 1024 * 1024);
			}
		} catch (FileStorageService.FileTooLargeException ex) {
			return "redirect:/thong-tin-doi?teamId=" + team.getId() + "&edit=1&logoTooLarge";
		} catch (FileStorageService.InvalidFileTypeException ex) {
			return "redirect:/thong-tin-doi?teamId=" + team.getId() + "&edit=1&logoInvalidType";
		} catch (Exception ex) {
			return "redirect:/thong-tin-doi?teamId=" + team.getId() + "&edit=1&logoError";
		}

		team.setName(name);
		team.setLogoUrl(nextLogoUrl);
		teamService.save(team);

		playerRepository.deleteByTeamId(team.getId());

		List<Player> players = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String n = memberNames != null && i < memberNames.size() ? memberNames.get(i) : null;
			if (n == null || n.trim().isBlank()) {
				continue;
			}

			Integer jersey = memberJerseys != null && i < memberJerseys.size() ? memberJerseys.get(i) : null;
			if (jersey != null && (jersey < 0 || jersey > 999)) {
				jersey = null;
			}

			String avatarUrl = existingMemberAvatars != null && i < existingMemberAvatars.size()
					? existingMemberAvatars.get(i)
					: null;
			try {
				MultipartFile avatarFile = memberAvatars != null && i < memberAvatars.length ? memberAvatars[i] : null;
				if (avatarFile != null && !avatarFile.isEmpty()) {
					avatarUrl = fileStorageService.storeValidatedImageUnderUploads(avatarFile, "players", 2L * 1024 * 1024);
				}
			} catch (Exception ex) {
				avatarUrl = null;
			}

			Player p = new Player(n.trim(), team);
			p.setJerseyNumber(jersey);
			p.setRole("Cầu thủ");
			p.setAvatarUrl(avatarUrl);
			players.add(p);
		}
		if (!players.isEmpty()) {
			playerRepository.saveAll(players);
		}

		return "redirect:/thong-tin-doi?teamId=" + team.getId();
	}

	private record TeamEditLock(boolean locked, String message) {
	}

	private String renderCreateTeamError(Model model, AppUser user, TeamCreateForm teamForm, BindingResult bindingResult) {
		attachCommonProfileModel(model, user);
		List<TeamCardView> teams = teamService.listByCaptain(user.getId()).stream()
				.map(t -> new TeamCardView(
						t.getId(),
						t.getName(),
						imageService.resolveTournamentCoverUrl(t.getLogoUrl()),
						(int) playerRepository.countByTeamId(t.getId())
				))
				.collect(Collectors.toList());
		model.addAttribute("teams", teams);
		model.addAttribute("canCreateTeam", teams.size() < 2);
		model.addAttribute("isCreate", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("teamForm", teamForm);
		model.addAttribute("org.springframework.validation.BindingResult.teamForm", bindingResult);
		ensureMemberSlots(model, List.of());
		return "user/profile/team-info";
	}

	private String renderEditTeamError(Model model, AppUser user, Team team, TeamCreateForm teamForm, BindingResult bindingResult) {
		attachCommonProfileModel(model, user);
		List<TeamCardView> teams = teamService.listByCaptain(user.getId()).stream()
				.map(t -> new TeamCardView(
						t.getId(),
						t.getName(),
						imageService.resolveTournamentCoverUrl(t.getLogoUrl()),
						(int) playerRepository.countByTeamId(t.getId())
				))
				.collect(Collectors.toList());
		model.addAttribute("teams", teams);
		model.addAttribute("canCreateTeam", teams.size() < 2);
		model.addAttribute("selectedTeam", team);
		List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId());
		model.addAttribute("members", members);
		model.addAttribute("isCreate", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("editTeamId", team.getId());
		model.addAttribute("teamForm", teamForm);
		model.addAttribute("org.springframework.validation.BindingResult.teamForm", bindingResult);
		ensureMemberSlots(model, members);
		return "user/profile/team-info";
	}

	private void ensureMemberSlots(Model model, List<Player> members) {
		List<MemberSlot> slots = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Player p = members != null && i < members.size() ? members.get(i) : null;
			String name = p == null ? "" : (p.getFullName() == null ? "" : p.getFullName());
			Integer jersey = p == null ? null : p.getJerseyNumber();
			String avatar = p == null ? null : p.getAvatarUrl();
			slots.add(new MemberSlot(name, jersey, imageService.resolveUserAvatarUrl(avatar)));
		}
		model.addAttribute("memberSlots", slots);
	}

	private void attachTeamDetailModel(Model model, Team team, Long tournamentId) {
		var registrations = tournamentRegistrationService.listApprovedByTeamId(team.getId());
		boolean hasRegistrations = registrations != null && !registrations.isEmpty();
		model.addAttribute("hasTeamRegistrations", hasRegistrations);

		if (!hasRegistrations) {
			model.addAttribute("scheduleRows", List.of());
			model.addAttribute("resultRows", List.of());
			model.addAttribute("recentResults", List.of());
			model.addAttribute("seasonCards", List.of());
			model.addAttribute("tournamentOptions", List.of());
			model.addAttribute("selectedTournamentId", null);
			model.addAttribute("analysisWin", 0);
			model.addAttribute("analysisDraw", 0);
			model.addAttribute("analysisLoss", 0);
			model.addAttribute("analysisLabels", List.of());
			model.addAttribute("analysisGoalsFor", List.of());
			model.addAttribute("analysisGoalsAgainst", List.of());
			return;
		}

		List<TournamentOption> tournamentOptions = registrations.stream()
				.map(r -> new TournamentOption(r.getTournament().getId(), r.getTournament().getName()))
				.distinct()
				.collect(Collectors.toList());
		model.addAttribute("tournamentOptions", tournamentOptions);

		List<Long> tournamentIds = registrations.stream()
				.map(r -> r.getTournament().getId())
				.distinct()
				.collect(Collectors.toList());

		Long selectedTournamentId = tournamentId != null && tournamentIds.contains(tournamentId) ? tournamentId : null;
		model.addAttribute("selectedTournamentId", selectedTournamentId);

		List<Match> matches;
		if (selectedTournamentId != null) {
			matches = matchRepository.findByTeamIdAndTournamentIdWithDetails(team.getId(), selectedTournamentId);
		} else {
			matches = matchRepository.findByTeamIdWithDetails(team.getId()).stream()
					.filter(m -> m.getTournament() != null && m.getTournament().getId() != null && tournamentIds.contains(m.getTournament().getId()))
					.collect(Collectors.toList());
		}

		List<ScheduleRow> scheduleRows = matches.stream()
				.filter(m -> m.getScheduledAt() != null && m.getStatus() != MatchStatus.FINISHED)
				.map(m -> new ScheduleRow(
						formatDate(m.getScheduledAt()),
						formatTime(m.getScheduledAt()),
						m.getTournament().getName(),
						opponentName(team.getId(), m),
						m.getRoundName() == null ? "" : m.getRoundName(),
						matchStatusLabel(m.getStatus())
				))
				.collect(Collectors.toList());
		model.addAttribute("scheduleRows", scheduleRows);

		List<ResultRow> resultRows = matches.stream()
				.filter(m -> m.getScheduledAt() != null && m.getStatus() == MatchStatus.FINISHED)
				.sorted((a, b) -> b.getScheduledAt().compareTo(a.getScheduledAt()))
				.map(m -> new ResultRow(
						formatDate(m.getScheduledAt()),
						m.getTournament().getName(),
						opponentName(team.getId(), m),
						scoreText(team.getId(), m),
						m.getRoundName() == null ? "" : m.getRoundName()
				))
				.collect(Collectors.toList());
		model.addAttribute("resultRows", resultRows);

		model.addAttribute("recentResults", resultRows.stream().limit(3).collect(Collectors.toList()));

		List<SeasonCard> seasonCards = buildSeasonCards(team.getId(), registrations);
		model.addAttribute("seasonCards", seasonCards);

		var analysis = buildGoalsAnalysis(team.getId(), matches);
		model.addAttribute("analysisWin", analysis.win());
		model.addAttribute("analysisDraw", analysis.draw());
		model.addAttribute("analysisLoss", analysis.loss());
		model.addAttribute("analysisLabels", analysis.labels());
		model.addAttribute("analysisGoalsFor", analysis.goalsFor());
		model.addAttribute("analysisGoalsAgainst", analysis.goalsAgainst());
	}

	private GoalsAnalysis buildGoalsAnalysis(Long teamId, List<Match> matches) {
		if (matches == null || matches.isEmpty()) {
			return new GoalsAnalysis(0, 0, 0, List.of(), List.of(), List.of());
		}

		List<Match> finished = matches.stream()
				.filter(m -> m != null && m.getStatus() == MatchStatus.FINISHED && m.getScheduledAt() != null)
				.sorted(Comparator.comparing(Match::getScheduledAt))
				.collect(Collectors.toList());

		int win = 0;
		int draw = 0;
		int loss = 0;
		List<String> labels = new ArrayList<>();
		List<Integer> gf = new ArrayList<>();
		List<Integer> ga = new ArrayList<>();

		for (Match m : finished) {
			Integer goalsFor = goalsFor(teamId, m);
			Integer goalsAgainst = goalsAgainst(teamId, m);
			if (goalsFor == null || goalsAgainst == null) continue;
			labels.add(formatDate(m.getScheduledAt()));
			gf.add(goalsFor);
			ga.add(goalsAgainst);
			if (goalsFor > goalsAgainst) win++;
			else if (goalsFor.equals(goalsAgainst)) draw++;
			else loss++;
		}
		return new GoalsAnalysis(win, draw, loss, labels, gf, ga);
	}

	private List<SeasonCard> buildSeasonCards(Long teamId, List<TournamentRegistration> registrations) {
		List<SeasonCard> seasons = new ArrayList<>();
		if (registrations == null) return seasons;

		Set<Long> seenTournamentIds = new HashSet<>();
		for (TournamentRegistration r : registrations) {
			if (r == null || r.getTournament() == null || r.getTournament().getId() == null) continue;
			Long tid = r.getTournament().getId();
			if (seenTournamentIds.contains(tid)) continue;
			seenTournamentIds.add(tid);

			Tournament t = r.getTournament();
			String statusLabel = tournamentStatusLabel(t.getStatus());
			String statusClass = tournamentStatusClass(t.getStatus());
			String achievementLabel = "Tham gia";
			String achievementClass = "badge--muted";

			boolean isWinner = isWinner(teamId, tid);
			if (isWinner) {
				achievementLabel = "Vô địch";
				achievementClass = "badge--success";
			} else if (isRunnerUp(teamId, tid)) {
				achievementLabel = "Á quân";
				achievementClass = "badge--pending";
			}

			seasons.add(new SeasonCard(tid, t.getName(), statusLabel, statusClass, achievementLabel, achievementClass));
		}
		seasons.sort(Comparator.comparing(SeasonCard::tournamentId).reversed());
		return seasons;
	}

	private boolean isWinner(Long teamId, Long tournamentId) {
		if (teamId == null || tournamentId == null) return false;
		Match finalMatch = matchRepository.findByTournamentIdWithDetails(tournamentId).stream()
				.filter(m -> m != null && m.getRoundName() != null && "Chung kết".equalsIgnoreCase(m.getRoundName().trim()))
				.findFirst()
				.orElse(null);
		if (finalMatch == null) return false;
		Integer gf = goalsFor(teamId, finalMatch);
		Integer ga = goalsAgainst(teamId, finalMatch);
		if (gf == null || ga == null) return false;
		return gf > ga;
	}

	private boolean isRunnerUp(Long teamId, Long tournamentId) {
		if (teamId == null || tournamentId == null) return false;
		Match finalMatch = matchRepository.findByTournamentIdWithDetails(tournamentId).stream()
				.filter(m -> m != null && m.getRoundName() != null && "Chung kết".equalsIgnoreCase(m.getRoundName().trim()))
				.findFirst()
				.orElse(null);
		if (finalMatch == null) return false;
		Integer gf = goalsFor(teamId, finalMatch);
		Integer ga = goalsAgainst(teamId, finalMatch);
		if (gf == null || ga == null) return false;
		return gf < ga;
	}

	private static String matchStatusLabel(MatchStatus status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case SCHEDULED -> "Sắp diễn ra";
			case LIVE -> "Đang diễn ra";
			case FINISHED -> "Đã kết thúc";
		};
	}

	private static String tournamentStatusLabel(TournamentStatus status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case UPCOMING -> "Sắp diễn ra";
			case LIVE -> "Đang diễn ra";
			case FINISHED -> "Đã kết thúc";
		};
	}

	private static String tournamentStatusClass(TournamentStatus status) {
		if (status == null) {
			return "badge--muted";
		}
		return switch (status) {
			case UPCOMING -> "badge--upcoming";
			case LIVE -> "badge--live";
			case FINISHED -> "badge--finished";
		};
	}

	private String opponentName(Long teamId, Match m) {
		if (m == null || teamId == null) return "";
		Team home = m.getHomeTeam();
		Team away = m.getAwayTeam();
		if (home != null && home.getId() != null && home.getId().equals(teamId)) {
			return away == null ? "" : (away.getName() == null ? "" : away.getName());
		}
		if (away != null && away.getId() != null && away.getId().equals(teamId)) {
			return home == null ? "" : (home.getName() == null ? "" : home.getName());
		}
		return "";
	}

	private Integer goalsFor(Long teamId, Match m) {
		if (m == null || teamId == null) return null;
		if (m.getHomeTeam() != null && teamId.equals(m.getHomeTeam().getId())) {
			return m.getHomeScore();
		}
		if (m.getAwayTeam() != null && teamId.equals(m.getAwayTeam().getId())) {
			return m.getAwayScore();
		}
		return null;
	}

	private Integer goalsAgainst(Long teamId, Match m) {
		if (m == null || teamId == null) return null;
		if (m.getHomeTeam() != null && teamId.equals(m.getHomeTeam().getId())) {
			return m.getAwayScore();
		}
		if (m.getAwayTeam() != null && teamId.equals(m.getAwayTeam().getId())) {
			return m.getHomeScore();
		}
		return null;
	}

	private String scoreText(Long teamId, Match m) {
		Integer gf = goalsFor(teamId, m);
		Integer ga = goalsAgainst(teamId, m);
		if (gf == null || ga == null) return "";
		return gf + " - " + ga;
	}

	private String formatDate(java.time.LocalDateTime dateTime) {
		if (dateTime == null) return "";
		return DATE_FMT.format(dateTime);
	}

	private String formatTime(java.time.LocalDateTime dateTime) {
		if (dateTime == null) return "";
		return TIME_FMT.format(dateTime);
	}

	public static class TeamCreateForm {
		@NotBlank(message = "Vui lòng nhập tên đội")
		private String teamName;

		public String getTeamName() { return teamName; }
		public void setTeamName(String teamName) { this.teamName = teamName; }
	}

	private record TeamCardView(Long id, String name, String logoUrl, int memberCount) {
	}

	private record MemberSlot(String name, Integer jerseyNumber, String avatarUrl) {
	}

	private record ScheduleRow(String date, String time, String tournamentName, String opponentName, String round, String status) {
	}

	private record ResultRow(String date, String tournamentName, String opponentName, String score, String round) {
	}

	private record TournamentOption(Long id, String name) {
	}

	private record SeasonCard(Long tournamentId, String tournamentName, String statusLabel, String statusClass, String achievementLabel, String achievementClass) {
	}

	private record GoalsAnalysis(int win, int draw, int loss, List<String> labels, List<Integer> goalsFor, List<Integer> goalsAgainst) {
	}
}

