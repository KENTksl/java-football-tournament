package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.MatchEvent;
import com.example.football_tourament_web.model.entity.MatchLineupSlot;
import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.MatchEventType;
import com.example.football_tourament_web.model.enums.PitchType;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TeamSide;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.model.enums.UserRole;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.MatchEventService;
import com.example.football_tourament_web.service.MatchLineupService;
import com.example.football_tourament_web.service.MatchService;
import com.example.football_tourament_web.service.TeamService;
import com.example.football_tourament_web.service.TournamentRegistrationService;
import com.example.football_tourament_web.service.TournamentService;
import com.example.football_tourament_web.service.UserService;
import com.example.football_tourament_web.service.TransactionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class AdminController {

	private final TournamentService tournamentService;
	private final TeamService teamService;
	private final MatchService matchService;
	private final MatchLineupService matchLineupService;
	private final MatchEventService matchEventService;
	private final UserService userService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final PlayerRepository playerRepository;
	private final TransactionService transactionService;

	public AdminController(
			TournamentService tournamentService,
			TeamService teamService,
			MatchService matchService,
			MatchLineupService matchLineupService,
			MatchEventService matchEventService,
			UserService userService,
			TournamentRegistrationService tournamentRegistrationService,
			PlayerRepository playerRepository,
			TransactionService transactionService
	) {
		this.tournamentService = tournamentService;
		this.teamService = teamService;
		this.matchService = matchService;
		this.matchLineupService = matchLineupService;
		this.matchEventService = matchEventService;
		this.userService = userService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.playerRepository = playerRepository;
		this.transactionService = transactionService;
	}

	@GetMapping({"/admin", "/admin/general-overview"})
	public String generalOverview(Model model) {
		model.addAttribute("totalTournaments", tournamentService.countTournaments());
		model.addAttribute("totalTeams", teamService.countTeams());
		model.addAttribute("activeTournaments", tournamentService.countTournamentsByStatus(TournamentStatus.LIVE));
		model.addAttribute("completedTournaments", tournamentService.countTournamentsByStatus(TournamentStatus.FINISHED));

		// For winners table
		model.addAttribute("recentWinners", tournamentService.getRecentWinners());

		// For chart: real counts for last 7 months
		List<Long> matchFrequency = matchService.getMatchFrequencyForLast7Months();
		model.addAttribute("matchFrequency", matchFrequency);

		return "admin/dashboard/general-overview";
	}

	@GetMapping({"/admin/admin-profile", "/admin/profile"})
	public String adminProfile(Model model, Principal principal) {
		String email = principal.getName();
		AppUser admin = userService.findByEmail(email).orElse(null);
		model.addAttribute("admin", admin);
		return "admin/profile/admin-profile";
	}

	@PostMapping("/admin/profile/save")
	public String saveAdminProfile(Principal principal,
								   @RequestParam("fullName") String fullName,
								   @RequestParam("phone") String phone,
								   @RequestParam("address") String address,
								   @RequestParam(value = "dob", required = false) String dob,
								   @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) {
		String email = principal.getName();
		AppUser admin = userService.findByEmail(email).orElse(null);
		if (admin != null) {
			admin.setFullName(fullName);
			admin.setPhone(phone);
			admin.setAddress(address);
			if (dob != null && !dob.isEmpty()) {
				admin.setDateOfBirth(java.time.LocalDate.parse(dob));
			}

			if (avatarFile != null && !avatarFile.isEmpty()) {
				try {
					String fileName = UUID.randomUUID().toString() + "_" + avatarFile.getOriginalFilename();
					Path uploadPath = resolveUploadDir("avatars");

					if (!Files.exists(uploadPath)) {
						Files.createDirectories(uploadPath);
					}

					try (var inputStream = avatarFile.getInputStream()) {
						Path filePath = uploadPath.resolve(fileName);
						Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
						String avatarPath = "/uploads/avatars/" + fileName;
						admin.setAvatar(avatarPath);
						admin.setAvatarUrl(avatarPath);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			userService.save(admin);
		}
		return "redirect:/admin/profile";
	}

	@GetMapping({"/admin/manage-tournament", "/admin/manage/tournament"})
	public String manageTournament(Model model) {
		List<Tournament> tournaments = tournamentService.listTournaments();
		Map<Long, Long> registeredTeamCounts = new HashMap<>();
		for (Tournament t : tournaments) {
			if (t == null || t.getId() == null) continue;
			registeredTeamCounts.put(t.getId(), tournamentRegistrationService.countRegisteredTeams(t.getId()));
		}
		model.addAttribute("tournaments", tournaments);
		model.addAttribute("registeredTeamCounts", registeredTeamCounts);
		return "admin/manage/manage-tournament";
	}

	@PostMapping("/admin/manage/tournament/add")
	public String addTournament(@RequestParam("name") String name,
								@RequestParam("organizer") String organizer,
								@RequestParam("mode") String mode,
								@RequestParam("pitchType") String pitchType,
								@RequestParam("teams") String teams,
								@RequestParam("startDate") String startDate,
								@RequestParam("endDate") String endDate,
								@RequestParam("description") String description,
								@RequestParam(value = "image", required = false) MultipartFile image) {
		Tournament tournament = new Tournament();
		tournament.setName(name);
		tournament.setOrganizer(organizer);
		
		if (mode.contains("Group Stage")) {
			tournament.setMode(TournamentMode.GROUP_STAGE);
		} else {
			tournament.setMode(TournamentMode.KNOCKOUT);
		}

		if (pitchType.contains("5")) {
			tournament.setPitchType(PitchType.PITCH_5);
		} else if (pitchType.contains("11")) {
			tournament.setPitchType(PitchType.PITCH_11);
		} else {
			tournament.setPitchType(PitchType.PITCH_7);
		}

		try {
			String teamCount = teams.split("/")[0];
			tournament.setTeamLimit(Integer.parseInt(teamCount));
		} catch (Exception e) {
			tournament.setTeamLimit(4);
		}

		try {
			if (startDate != null && !startDate.isEmpty()) {
				tournament.setStartDate(LocalDate.parse(startDate));
			}
			if (endDate != null && !endDate.isEmpty()) {
				tournament.setEndDate(LocalDate.parse(endDate));
			}
		} catch (Exception e) {
			// fallback to current date or handle error
			tournament.setStartDate(LocalDate.now());
			tournament.setEndDate(LocalDate.now().plusMonths(1));
		}

		tournament.setDescription(description);
		tournament.setStatus(calculateStatus(tournament.getStartDate(), tournament.getEndDate()));

		if (image != null && !image.isEmpty()) {
			tournament.setImageUrl(saveFile(image, "tournaments", "/uploads/tournaments/"));
		}

		tournamentService.save(tournament);
		return "redirect:/admin/manage/tournament";
	}

	@PostMapping("/admin/manage/tournament/edit/{id}")
	public String editTournament(@PathVariable("id") Long id,
								 @RequestParam("name") String name,
								 @RequestParam("organizer") String organizer,
								 @RequestParam("mode") String mode,
								 @RequestParam("pitchType") String pitchType,
								 @RequestParam("teams") String teams,
								 @RequestParam("startDate") String startDate,
								 @RequestParam("endDate") String endDate,
								 @RequestParam("description") String description,
								 @RequestParam(value = "image", required = false) MultipartFile image) {
		Tournament tournament = tournamentService.findById(id).orElse(null);
		if (tournament != null) {
			tournament.setName(name);
			tournament.setOrganizer(organizer);
			
			if (mode.contains("Group Stage")) {
				tournament.setMode(TournamentMode.GROUP_STAGE);
			} else {
				tournament.setMode(TournamentMode.KNOCKOUT);
			}

			if (pitchType.contains("5")) {
				tournament.setPitchType(PitchType.PITCH_5);
			} else if (pitchType.contains("11")) {
				tournament.setPitchType(PitchType.PITCH_11);
			} else {
				tournament.setPitchType(PitchType.PITCH_7);
			}

			try {
				String teamCount = teams.split("/")[0];
				tournament.setTeamLimit(Integer.parseInt(teamCount));
			} catch (Exception e) {
				// keep old value or default
			}

			try {
				if (startDate != null && !startDate.isEmpty()) {
					tournament.setStartDate(LocalDate.parse(startDate));
				}
				if (endDate != null && !endDate.isEmpty()) {
					tournament.setEndDate(LocalDate.parse(endDate));
				}
			} catch (Exception e) {
				// keep old values or set defaults
			}

			tournament.setDescription(description);
			tournament.setStatus(calculateStatus(tournament.getStartDate(), tournament.getEndDate()));

			if (image != null && !image.isEmpty()) {
				tournament.setImageUrl(saveFile(image, "tournaments", "/uploads/tournaments/"));
			}

			tournamentService.save(tournament);
		}
		return "redirect:/admin/manage/tournament";
	}

	private TournamentStatus calculateStatus(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null) {
			return TournamentStatus.UPCOMING;
		}
		LocalDate today = LocalDate.now();
		if (today.isBefore(startDate)) {
			return TournamentStatus.UPCOMING;
		} else if (today.isAfter(endDate)) {
			return TournamentStatus.FINISHED;
		} else {
			return TournamentStatus.LIVE;
		}
	}

	@PostMapping("/admin/manage/tournament/delete/{id}")
	public String deleteTournament(@PathVariable("id") Long id) {
		tournamentService.deleteById(id);
		return "redirect:/admin/manage/tournament";
	}

	private String saveFile(MultipartFile file, String folder, String publicPath) {
		try {
			String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
			Path uploadPath = resolveUploadDir(folder);

			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			try (var inputStream = file.getInputStream()) {
				Path filePath = uploadPath.resolve(fileName);
				Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
				return publicPath + fileName;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Path resolveUploadDir(String folder) {
		return Paths.get(System.getProperty("user.home"), ".football_tournament_web", "uploads", folder).toAbsolutePath().normalize();
	}


	@GetMapping({"/admin/manage-user", "/admin/manage/user"})
	public String manageUser(Model model) {
		List<AppUser> users = userService.listUsersByRole(UserRole.USER);
		model.addAttribute("users", users);
		return "admin/manage/manage-user";
	}

	@GetMapping("/admin/manage")
	public String manageHome() {
		return "redirect:/admin/manage/tournament";
	}

	@GetMapping("/admin/team-management")
	public String teamManagement(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
			Model model
	) {
		List<Tournament> tournaments = tournamentService.listTournaments();
		model.addAttribute("tournaments", tournaments);

		Long selectedTournamentId = tournamentId;
		if (selectedTournamentId == null && !tournaments.isEmpty()) {
			selectedTournamentId = tournaments.get(0).getId();
		}
		model.addAttribute("selectedTournamentId", selectedTournamentId);

		RegistrationStatus selectedStatus = parseRegistrationStatus(status);
		model.addAttribute("selectedStatus", selectedStatus == null ? "ALL" : selectedStatus.name());

		List<TeamRegistrationRow> rows = buildTeamRegistrationRows(selectedTournamentId, selectedStatus);
		model.addAttribute("registrationRows", rows);
		return "admin/team/team-management";
	}

	@PostMapping("/admin/team-management/update-status")
	public String updateTeamRegistrationStatus(
			@RequestParam("registrationId") Long registrationId,
			@RequestParam("tournamentId") Long tournamentId,
			@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
			@RequestParam("targetStatus") String targetStatus,
			RedirectAttributes redirectAttributes
	) {
		RegistrationStatus newStatus = parseRegistrationStatus(targetStatus);
		if (newStatus == null) {
			redirectAttributes.addFlashAttribute("teamManageMessage", "Trạng thái cập nhật không hợp lệ");
			return "redirect:/admin/team-management?tournamentId=" + tournamentId + "&status=" + status;
		}
		var registration = tournamentRegistrationService.findById(registrationId).orElse(null);
		if (registration == null || registration.getTournament() == null || registration.getTournament().getId() == null
				|| !registration.getTournament().getId().equals(tournamentId)) {
			redirectAttributes.addFlashAttribute("teamManageMessage", "Không tìm thấy hồ sơ đăng ký cần cập nhật");
			return "redirect:/admin/team-management?tournamentId=" + tournamentId + "&status=" + status;
		}
		registration.setStatus(newStatus);
		if (newStatus != RegistrationStatus.APPROVED) {
			registration.setGroupName(null);
		}
		tournamentRegistrationService.save(registration);
		redirectAttributes.addFlashAttribute("teamManageMessage", "Đã cập nhật trạng thái hồ sơ");
		return "redirect:/admin/team-management?tournamentId=" + tournamentId + "&status=" + status;
	}

	@GetMapping("/admin/team-detail")
	public String teamDetail() {
		return "admin/team/team-detail";
	}

	@GetMapping("/admin/invoice-management")
	public String invoiceManagement(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "sort", required = false) String sort,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "page", defaultValue = "1") int page,
			Model model) {
		List<Transaction> transactions;
		
		if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
			try {
				com.example.football_tourament_web.model.enums.TransactionStatus targetStatus = com.example.football_tourament_web.model.enums.TransactionStatus.valueOf(status.toUpperCase());
				transactions = transactionService.listAll().stream()
						.filter(t -> t.getStatus() == targetStatus)
						.collect(java.util.stream.Collectors.toList());
			} catch (IllegalArgumentException e) {
				transactions = transactionService.listAll();
			}
		} else {
			transactions = transactionService.listAll();
		}

		if (search != null && !search.trim().isEmpty()) {
			String query = search.toLowerCase().trim();
			transactions = transactions.stream()
					.filter(t -> (t.getCode() != null && t.getCode().toLowerCase().contains(query)) ||
							(t.getUser() != null && t.getUser().getFullName() != null && t.getUser().getFullName().toLowerCase().contains(query)))
					.collect(java.util.stream.Collectors.toList());
		}

		if ("time_asc".equalsIgnoreCase(sort)) {
			transactions.sort(Comparator.comparing(Transaction::getCreatedAt));
		} else if ("time_desc".equalsIgnoreCase(sort)) {
			transactions.sort(Comparator.comparing(Transaction::getCreatedAt).reversed());
		} else if ("value_asc".equalsIgnoreCase(sort)) {
			transactions.sort(Comparator.comparing(Transaction::getAmount));
		} else if ("value_desc".equalsIgnoreCase(sort)) {
			transactions.sort(Comparator.comparing(Transaction::getAmount).reversed());
		}

		int pageSize = 6;
		int totalItems = transactions.size();
		int totalPages = (int) Math.ceil((double) totalItems / pageSize);
		
		// Ensure current page is within valid range
		if (page < 1) page = 1;
		if (totalPages > 0 && page > totalPages) page = totalPages;

		int start = (page - 1) * pageSize;
		int end = Math.min(start + pageSize, totalItems);
		
		List<Transaction> pagedTransactions = (totalItems > 0 && start < totalItems) 
				? transactions.subList(start, end) 
				: Collections.emptyList();

		model.addAttribute("transactions", pagedTransactions);
		model.addAttribute("currentStatus", status != null ? status : "ALL");
		model.addAttribute("currentSort", sort != null ? sort : "time_desc");
		model.addAttribute("currentSearch", search != null ? search : "");
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("pageSize", pageSize);
		
		return "admin/invoice/invoice-management";
	}

	@GetMapping("/admin/tournament-bracket")
	public String tournamentBracket(@RequestParam(value = "id", required = false) Long id, Model model) {
		if (id == null) {
			return "redirect:/admin/manage/tournament";
		}
		Tournament tournament = tournamentService.findById(id).orElse(null);
		if (tournament == null) {
			return "redirect:/admin/manage/tournament";
		}
		applyTournamentContext(model, tournament);
		return "admin/tournament/tournament-bracket";
	}

	@GetMapping({"/admin/manage/user-detail"})
	public String manageUserDetail() {
		return "admin/manage/user-detail";
	}

	@GetMapping({"/admin/manage/user-team-detail"})
	public String manageUserTeamDetail() {
		return "admin/manage/user-team-detail";
	}

	@GetMapping({"/admin/manage/user-transaction-history"})
	public String manageUserTransactionHistory() {
		return "admin/manage/user-transaction-history";
	}


	@GetMapping("/admin/general-information")
	public String adminInformation(@RequestParam(value = "id", required = false) Long id, Model model) {
		if (id == null) {
			return "redirect:/admin/manage/tournament";
		}
		Tournament tournament = tournamentService.findById(id).orElse(null);
		if (tournament == null) {
			return "redirect:/admin/manage/tournament";
		}
		applyTournamentContext(model, tournament);
		model.addAttribute("organizerName", tournament.getOrganizer());
		model.addAttribute("mode", displayMode(tournament.getMode()));
		model.addAttribute("teamFormat", displayTeamFormat(tournament.getTeamLimit()));
		model.addAttribute("status", displayStatus(tournament.getStatus()));
		model.addAttribute("description", tournament.getDescription());
		return "admin/tournament/general-information";
	}

	@GetMapping("/admin/team-list")
	public String adminTeamList(
			@RequestParam(value = "id", required = false) Long id,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size,
			Model model
	) {
		if (id == null) {
			return "redirect:/admin/manage/tournament";
		}
		Tournament tournament = tournamentService.findById(id).orElse(null);
		if (tournament == null) {
			return "redirect:/admin/manage/tournament";
		}
		applyTournamentContext(model, tournament);
		List<TeamListItem> allTeams = buildTeamListItems(id);
		PagedResult<TeamListItem> paged = paginate(allTeams, page, size);
		model.addAttribute("teams", paged.items());
		model.addAttribute("currentPage", paged.currentPage());
		model.addAttribute("pageSize", paged.pageSize());
		model.addAttribute("totalPages", paged.totalPages());
		return "admin/tournament/team-list";
	}

	@GetMapping("/admin/team-list/team-players")
	@ResponseBody
	public List<PlayerDto> teamPlayers(@RequestParam(value = "teamId", required = false) Long teamId) {
		if (teamId == null) return List.of();
		List<PlayerDto> players = new ArrayList<>();
		for (Player p : playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId)) {
			players.add(new PlayerDto(p.getId(), p.getFullName(), p.getJerseyNumber(), p.getPosition(), p.getAvatarUrl()));
		}
		return players;
	}

	@GetMapping("/admin/match-history")
	public String adminMatchHistory(
			@RequestParam(value = "id", required = false) Long id,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "tab", required = false) String tab,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size,
			Model model
	) {
		if (id == null) {
			return "redirect:/admin/manage/tournament";
		}
		Tournament tournament = tournamentService.findById(id).orElse(null);
		if (tournament == null) {
			return "redirect:/admin/manage/tournament";
		}
		applyTournamentContext(model, tournament);
		model.addAttribute("tournamentMode", tournament.getMode());
		model.addAttribute("tournamentTeamLimit", tournament.getTeamLimit());
		model.addAttribute("tournamentPitchType", tournament.getPitchType());
		List<Match> allMatches = matchService.listByTournamentIdWithDetails(id);
		List<Match> knockoutOnly = new ArrayList<>();
		for (Match m : allMatches) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim().toLowerCase();
			if (rn.startsWith("bảng")) continue;
			knockoutOnly.add(m);
		}

		List<Match> knockoutSource = tournament.getMode() == TournamentMode.KNOCKOUT ? allMatches : knockoutOnly;
		PagedResult<Match> paged = paginate(knockoutSource, page, size);
		model.addAttribute("knockoutMatches", paged.items());
		model.addAttribute("currentPage", paged.currentPage());
		model.addAttribute("pageSize", paged.pageSize());
		model.addAttribute("totalPages", paged.totalPages());
		model.addAttribute("pairingLocked", !allMatches.isEmpty());

		if (tournament.getMode() == TournamentMode.GROUP_STAGE) {
			Map<String, List<GroupTeamRow>> groupTeams = new HashMap<>();
			groupTeams.put("A", new ArrayList<>());
			groupTeams.put("B", new ArrayList<>());
			groupTeams.put("C", new ArrayList<>());
			groupTeams.put("D", new ArrayList<>());

			List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(id);
			List<Long> seenTeamIds = new ArrayList<>();
			boolean allAssigned = true;
			int teamCount = 0;

			for (TournamentRegistration r : registrations) {
				if (r == null) continue;
				if (r.getStatus() == RegistrationStatus.REJECTED) continue;
				if (r.getTeam() == null || r.getTeam().getId() == null) continue;

				Long teamId = r.getTeam().getId();
				if (seenTeamIds.contains(teamId)) continue;
				seenTeamIds.add(teamId);
				teamCount++;

				String g = r.getGroupName() == null ? "" : r.getGroupName().trim().toUpperCase();
				if (g.isBlank()) {
					allAssigned = false;
					continue;
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
						matchesForGroups = matchService.listByTournamentIdWithDetails(id);
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
				if (r.getStatus() == RegistrationStatus.REJECTED) continue;
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

			model.addAttribute("groupTeams", groupTeams);
			model.addAttribute("groupMatches", groupMatches);
			model.addAttribute("groupingLocked", groupingLocked);
			model.addAttribute("groupingReady", readyToAssign);

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
					boolean created = matchService.generateQuarterFinalsFromGroupsIfReady(id);
					if (created) {
						List<Match> refreshed = matchService.listByTournamentIdWithDetails(id);
						List<Match> refreshedKnockout = new ArrayList<>();
						for (Match m : refreshed) {
							if (m == null || m.getRoundName() == null) continue;
							String rn = m.getRoundName().trim().toLowerCase();
							if (rn.startsWith("bảng")) continue;
							refreshedKnockout.add(m);
						}
						PagedResult<Match> p2 = paginate(refreshedKnockout, page, size);
						model.addAttribute("knockoutMatches", p2.items());
						model.addAttribute("currentPage", p2.currentPage());
						model.addAttribute("pageSize", p2.pageSize());
						model.addAttribute("totalPages", p2.totalPages());
					}
				}
			}
		}

		if (matchId != null) {
			Match selectedMatch = matchService.findByIdWithDetails(matchId).orElse(null);
			if (selectedMatch != null && selectedMatch.getTournament() != null && selectedMatch.getTournament().getId() != null
					&& id.equals(selectedMatch.getTournament().getId())) {
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

				model.addAttribute("selectedMatch", selectedMatch);
				model.addAttribute("selectedTab", tab == null ? "" : tab);
				model.addAttribute("pitchSlots", pitchSlots);
				model.addAttribute("selectedHomePlayers", homePlayers);
				model.addAttribute("selectedAwayPlayers", awayPlayers);
				model.addAttribute("homeLineupByIndex", homeLineupByIndex);
				model.addAttribute("awayLineupByIndex", awayLineupByIndex);
				model.addAttribute("assignedHomePlayerIds", assignedHomePlayerIds);
				model.addAttribute("assignedAwayPlayerIds", assignedAwayPlayerIds);
				model.addAttribute("matchEventRows", eventRows);
			}
		}
		return "admin/tournament/match-history";
	}

	@PostMapping("/admin/match-history/random-groups")
	public String randomGroups(@RequestParam(value = "tournamentId", required = false) Long tournamentId) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return "redirect:/admin/manage/tournament";
		}

		if (tournament.getMode() != TournamentMode.GROUP_STAGE) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Integer teamLimit = tournament.getTeamLimit();
		if (teamLimit == null || teamLimit != 16) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<TournamentRegistration> uniqueRegs = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration r : registrations) {
			if (r == null) continue;
			if (r.getStatus() == RegistrationStatus.REJECTED) continue;
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

	@GetMapping("/admin/match-history/match-players")
	@ResponseBody
	public MatchPlayersResponse matchPlayers(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId
	) {
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

	@PostMapping("/admin/match-history/save-lineup")
	@ResponseBody
	public void saveLineup(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "lineupJson", required = false) String lineupJson
	) {
		if (tournamentId == null || matchId == null) {
			return;
		}
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

	@PostMapping("/admin/match-history/save-events")
	@ResponseBody
	public void saveEvents(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "eventsJson", required = false) String eventsJson
	) {
		if (tournamentId == null || matchId == null) {
			return;
		}
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
	@PostMapping("/admin/match-history/random-pair")
	public String randomPairMatchHistory(@RequestParam(value = "tournamentId", required = false) Long tournamentId) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return "redirect:/admin/manage/tournament";
		}

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

		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<Team> teams = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;
			if (registration.getStatus() == RegistrationStatus.REJECTED) continue;
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

	@PostMapping("/admin/match-history/save-schedule")
	public String saveMatchSchedule(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "time", required = false) String time,
			@RequestParam(value = "location", required = false) String location,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}
		if (matchId == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Match match = matchService.findById(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		String nextLocation = location == null ? "" : location.trim();
		match.setLocation(nextLocation.isBlank() ? null : nextLocation);

		LocalDateTime scheduledAt = null;
		try {
			String d = date == null ? "" : date.trim();
			String t = time == null ? "" : time.trim();
			if (!d.isBlank() && !t.isBlank()) {
				scheduledAt = LocalDate.parse(d).atTime(LocalTime.parse(t));
			}
		} catch (Exception ignored) {
			scheduledAt = null;
		}

		match.setScheduledAt(scheduledAt);
		matchService.save(match);

		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=schedule";
	}

	@PostMapping("/admin/match-history/save-score")
	public String saveMatchScore(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "homeScore", required = false) Integer homeScore,
			@RequestParam(value = "awayScore", required = false) Integer awayScore,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}
		if (matchId == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Match match = matchService.findById(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		match.setHomeScore(homeScore);
		match.setAwayScore(awayScore);
		matchService.save(match);

		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=score";
	}

	@PostMapping("/admin/match-history/save-lineup-form")
	public String saveMatchLineup(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam Map<String, String> params,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}
		if (matchId == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}
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
			if (slotDef == null) continue;
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

	@PostMapping("/admin/match-history/add-event")
	public String addMatchEvent(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "teamSide", required = false) TeamSide teamSide,
			@RequestParam(value = "minute", required = false) Integer minute,
			@RequestParam(value = "type", required = false) MatchEventType type,
			@RequestParam(value = "playerId", required = false) Long playerId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}
		if (matchId == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}
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

	@PostMapping("/admin/match-history/delete-event")
	public String deleteMatchEvent(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "eventId", required = false) Long eventId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}
		if (matchId == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}
		if (eventId != null) {
			matchEventService.deleteById(eventId);
		}
		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=timeline&page=" + page + "&size=" + size + "&saved=event";
	}

	@PostMapping("/admin/match-history/sync-score-from-events")
	public String syncScoreFromEvents(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}
		if (matchId == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}
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

	@PostMapping("/admin/match-history/finish-match")
	public String finishMatch(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		if (tournamentId == null) {
			return "redirect:/admin/manage/tournament";
		}
		if (matchId == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		Match match = matchService.findById(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

		match.setStatus(MatchStatus.FINISHED);
		matchService.save(match);
		matchService.generateNextKnockoutRoundIfReady(tournamentId, match.getRoundName());
		matchService.generateQuarterFinalsFromGroupsIfReady(tournamentId);
		return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=finish";
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

	private record PagedResult<T>(List<T> items, int currentPage, int pageSize, int totalPages) {
	}

	public record PlayerDto(Long id, String fullName, Integer jerseyNumber, String position, String avatarUrl) {
	}

	public record MatchPlayersResponse(List<PlayerDto> homePlayers, List<PlayerDto> awayPlayers, String lineupJson, String eventsJson) {
	}

	public record PitchSlot(int index, int top, int left, String label) {
	}

	public record MatchEventRow(Long id, Integer minute, String teamName, String playerName, MatchEventType type) {
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

	private void applyTournamentContext(Model model, Tournament tournament) {
		model.addAttribute("tournamentId", tournament.getId());
		model.addAttribute("tournamentName", tournament.getName());
	}

	private String displayMode(TournamentMode mode) {
		if (mode == null) return "";
		return mode == TournamentMode.GROUP_STAGE ? "Chia bảng đấu (Group Stage)" : "Knockout";
	}

	private String displayTeamFormat(Integer teamLimit) {
		if (teamLimit == null) return "";
		return teamLimit + " v " + teamLimit;
	}

	private String displayStatus(TournamentStatus status) {
		if (status == null) return "";
		return switch (status) {
			case UPCOMING -> "Sắp diễn ra";
			case LIVE -> "Đang diễn ra";
			case FINISHED -> "Hoàn thành";
		};
	}

	private RegistrationStatus parseRegistrationStatus(String raw) {
		if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
			return null;
		}
		try {
			return RegistrationStatus.valueOf(raw.toUpperCase());
		} catch (Exception ex) {
			return null;
		}
	}

	private List<TeamRegistrationRow> buildTeamRegistrationRows(Long tournamentId, RegistrationStatus statusFilter) {
		if (tournamentId == null) {
			return List.of();
		}
		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<TeamRegistrationRow> rows = new ArrayList<>();
		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;
			if (statusFilter != null && registration.getStatus() != statusFilter) continue;
			if (registration.getTeam() == null || registration.getTeam().getId() == null) continue;
			var captain = registration.getTeam().getCaptain();
			String representative = captain == null || captain.getFullName() == null ? "Chưa cập nhật" : captain.getFullName();
			String phone = captain == null || captain.getPhone() == null ? "Chưa cập nhật" : captain.getPhone();
			long memberCount = playerRepository.countByTeamId(registration.getTeam().getId());
			rows.add(new TeamRegistrationRow(
					registration.getId(),
					formatDate(registration.getCreatedAt()),
					registration.getTeam().getName(),
					representative,
					phone,
					memberCount,
					registration.getStatus()
			));
		}
		rows.sort(Comparator.comparing(TeamRegistrationRow::submittedDateText, Comparator.nullsLast(String::compareTo)).reversed());
		return rows;
	}

	private String formatDate(java.time.Instant instant) {
		if (instant == null) return "";
		return DateTimeFormatter.ofPattern("dd/MM/yyyy")
				.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
	}

	private List<TeamListItem> buildTeamListItems(Long tournamentId) {
		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<TeamListItem> teams = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;
			if (registration.getStatus() == RegistrationStatus.REJECTED) continue;
			if (registration.getTeam() == null || registration.getTeam().getId() == null) continue;

			Long teamId = registration.getTeam().getId();
			if (seenTeamIds.contains(teamId)) continue;
			seenTeamIds.add(teamId);

			long memberCount = playerRepository.countByTeamId(teamId);
			teams.add(new TeamListItem(teamId, registration.getTeam().getName(), memberCount));
		}

		return teams;
	}

	public static final class TeamListItem {
		private final Long id;
		private final String name;
		private final long memberCount;

		public TeamListItem(Long id, String name, long memberCount) {
			this.id = id;
			this.name = name;
			this.memberCount = memberCount;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public long getMemberCount() {
			return memberCount;
		}
	}

	public record TeamRegistrationRow(
			Long registrationId,
			String submittedDateText,
			String teamName,
			String representativeName,
			String phone,
			long memberCount,
			RegistrationStatus status
	) {
		public String statusLabel() {
			if (status == null) return "Không xác định";
			return switch (status) {
				case PENDING -> "Chờ duyệt";
				case APPROVED -> "Đã duyệt";
				case REJECTED -> "Đã hủy";
			};
		}

		public String statusClass() {
			if (status == RegistrationStatus.APPROVED) return "badge--approved";
			if (status == RegistrationStatus.REJECTED) return "badge--rejected";
			return "badge--pending";
		}
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

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public long getMemberCount() {
			return memberCount;
		}

		public int getPoints() {
			return points;
		}

		public int getGoalsFor() {
			return goalsFor;
		}

		public int getGoalsAgainst() {
			return goalsAgainst;
		}

		public int getGoalDiff() {
			return goalsFor - goalsAgainst;
		}
	}

}

