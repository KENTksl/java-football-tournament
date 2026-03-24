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
import com.example.football_tourament_web.model.entity.ContactMessage;
import com.example.football_tourament_web.model.enums.MatchEventType;
import com.example.football_tourament_web.model.enums.PitchType;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TeamSide;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.model.enums.UserRole;
import com.example.football_tourament_web.model.enums.UserStatus;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.MatchEventService;
import com.example.football_tourament_web.service.MatchLineupService;
import com.example.football_tourament_web.service.MatchService;
import com.example.football_tourament_web.service.TeamService;
import com.example.football_tourament_web.service.ContactMessageService;
import com.example.football_tourament_web.service.TournamentRegistrationService;
import com.example.football_tourament_web.service.TournamentService;
import com.example.football_tourament_web.service.UserService;
import com.example.football_tourament_web.service.TransactionService;
import com.example.football_tourament_web.service.TransactionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HashSet;
import java.util.Locale;
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
	private final ContactMessageService contactMessageService;

	public AdminController(
			TournamentService tournamentService,
			TeamService teamService,
			MatchService matchService,
			MatchLineupService matchLineupService,
			MatchEventService matchEventService,
			UserService userService,
			TournamentRegistrationService tournamentRegistrationService,
			PlayerRepository playerRepository,
			TransactionService transactionService,
			ContactMessageService contactMessageService
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
		this.contactMessageService = contactMessageService;
	}

	@ModelAttribute
	public void attachAdminTopbarModel(Model model) {
		try {
			List<AdminTopbarMessage> messages = contactMessageService.listRecent(5).stream()
					.map(m -> new AdminTopbarMessage(
							m.getId(),
							m.getName(),
							m.getEmail(),
							m.getMessage(),
							formatDateTime(m.getCreatedAt())
					))
					.toList();
			model.addAttribute("adminTopMessages", messages);
			model.addAttribute("adminUnreadMessageCount", contactMessageService.countUnread());
		} catch (Exception ex) {
			model.addAttribute("adminTopMessages", List.of());
			model.addAttribute("adminUnreadMessageCount", 0L);
		}

		List<AdminTopbarNotification> notifications = new ArrayList<>();
		try {
			for (TournamentRegistration r : tournamentRegistrationService.listRecentPendingWithDetails(5)) {
				String teamName = r.getTeam() == null ? "Đội" : (r.getTeam().getName() == null ? "Đội" : r.getTeam().getName());
				String tournamentName = r.getTournament() == null ? "giải đấu" : (r.getTournament().getName() == null ? "giải đấu" : r.getTournament().getName());
				String title = "Đội đăng ký giải";
				String detail = teamName + " • " + tournamentName;
				String href = "/admin/team-detail?id=" + r.getId();
				notifications.add(new AdminTopbarNotification(title, detail, formatDateTime(r.getCreatedAt()), href));
			}
			model.addAttribute("adminPendingRegistrationCount", tournamentRegistrationService.countPending());
		} catch (Exception ex) {
			model.addAttribute("adminPendingRegistrationCount", 0L);
		}

		try {
			List<Transaction> txs = transactionService.listAll().stream().limit(5).toList();
			for (Transaction t : txs) {
				String userName = t.getUser() == null ? "Người dùng" : (t.getUser().getFullName() == null ? "Người dùng" : t.getUser().getFullName());
				String title = "Giao dịch mới";
				String detail = userName + " • " + (formatMoney(t.getAmount()) == null ? "" : formatMoney(t.getAmount()));
				String href = "/admin/invoice-management?status=ALL&search=" + (t.getCode() == null ? "" : t.getCode());
				notifications.add(new AdminTopbarNotification(title, detail, formatDateTime(t.getCreatedAt()), href));
			}
		} catch (Exception ex) {
			// ignore, keep notifications list possibly empty
		}
		model.addAttribute("adminTopNotifications", notifications);
	}

	@GetMapping("/admin/api/topbar/messages")
	@ResponseBody
	public ResponseEntity<AdminTopbarMessagesResponse> topbarMessages() {
		try {
			List<AdminTopbarMessage> messages = contactMessageService.listRecent(5).stream()
					.map(m -> new AdminTopbarMessage(
							m.getId(),
							m.getName(),
							m.getEmail(),
							m.getMessage(),
							formatDateTime(m.getCreatedAt())
					))
					.toList();
			long unread = contactMessageService.countUnread();
			return ResponseEntity.ok(new AdminTopbarMessagesResponse(unread, messages));
		} catch (Exception ex) {
			return ResponseEntity.ok(new AdminTopbarMessagesResponse(0, List.of()));
		}
	}

	@GetMapping("/admin/api/topbar/notifications")
	@ResponseBody
	public ResponseEntity<AdminTopbarNotificationsResponse> topbarNotifications() {
		try {
			List<AdminTopbarNotification> notifications = new ArrayList<>();
			for (TournamentRegistration r : tournamentRegistrationService.listRecentPendingWithDetails(5)) {
				String teamName = r.getTeam() == null ? "Đội" : (r.getTeam().getName() == null ? "Đội" : r.getTeam().getName());
				String tournamentName = r.getTournament() == null ? "giải đấu" : (r.getTournament().getName() == null ? "giải đấu" : r.getTournament().getName());
				String title = "Đội đăng ký giải";
				String detail = teamName + " • " + tournamentName;
				String href = "/admin/team-detail?id=" + r.getId();
				notifications.add(new AdminTopbarNotification(title, detail, formatDateTime(r.getCreatedAt()), href));
			}

			List<Transaction> txs = transactionService.listAll().stream().limit(5).toList();
			for (Transaction t : txs) {
				String userName = t.getUser() == null ? "Người dùng" : (t.getUser().getFullName() == null ? "Người dùng" : t.getUser().getFullName());
				String title = "Giao dịch mới";
				String detail = userName + " • " + (formatMoney(t.getAmount()) == null ? "" : formatMoney(t.getAmount()));
				String href = "/admin/invoice-management?status=ALL&search=" + (t.getCode() == null ? "" : t.getCode());
				notifications.add(new AdminTopbarNotification(title, detail, formatDateTime(t.getCreatedAt()), href));
			}

			long pending = tournamentRegistrationService.countPending();
			return ResponseEntity.ok(new AdminTopbarNotificationsResponse(pending, notifications));
		} catch (Exception ex) {
			return ResponseEntity.ok(new AdminTopbarNotificationsResponse(0, List.of()));
		}
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
		List<AdminTournamentRow> tournamentRows = tournaments.stream()
				.filter(t -> t != null && t.getId() != null)
				.map(t -> {
					long registered = registeredTeamCounts.getOrDefault(t.getId(), 0L);
					int limit = t.getTeamLimit() == null ? 0 : t.getTeamLimit();
					String teamCountText = registered + "/" + limit;
					String modeLabel = t.getMode() == TournamentMode.GROUP_STAGE ? "Chia bảng đấu (Group Stage)" : "Knockout";
					TournamentStatus s = t.getStatus();
					String statusLabel = s == TournamentStatus.UPCOMING ? "Sắp diễn ra" : (s == TournamentStatus.LIVE ? "Đang đá" : "Đã kết thúc");
					String statusClass = s == TournamentStatus.UPCOMING ? "status-badge--upcoming" : (s == TournamentStatus.LIVE ? "status-badge--live" : "status-badge--finished");
					return new AdminTournamentRow(
							t.getId(),
							t.getName(),
							t.getOrganizer(),
							modeLabel,
							teamCountText,
							statusLabel,
							statusClass,
							t.getMode() == null ? null : t.getMode().name(),
							t.getPitchType() == null ? null : t.getPitchType().name(),
							t.getTeamLimit(),
							t.getStartDate() == null ? null : t.getStartDate().toString(),
							t.getEndDate() == null ? null : t.getEndDate().toString(),
							t.getDescription()
					);
				})
				.toList();
		model.addAttribute("tournamentRows", tournamentRows);
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
		return Paths.get("src", "main", "resources", "static", "uploads", folder).toAbsolutePath().normalize();
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
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "page", defaultValue = "1") int page,
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

		List<TeamRegistrationRow> allRows = buildTeamRegistrationRows(selectedTournamentId, selectedStatus, search);
		
		// Pagination
		int size = 10;
		PagedResult<TeamRegistrationRow> paged = paginate(allRows, page, size);
		
		model.addAttribute("registrationRows", paged.getItems());
		model.addAttribute("currentPage", paged.getCurrentPage());
		model.addAttribute("totalPages", paged.getTotalPages());
		model.addAttribute("currentSearch", search);
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
	public String teamDetail(
			@RequestParam(value = "id", required = false) Long registrationId,
			Model model
	) {
		if (registrationId == null) {
			return "redirect:/admin/team-management";
		}
		var regOpt = tournamentRegistrationService.findByIdWithDetails(registrationId);
		var registration = regOpt.orElse(null);
		if (registration == null || registration.getTeam() == null || registration.getTeam().getId() == null) {
			return "redirect:/admin/team-management";
		}
		var team = registration.getTeam();
		Long teamId = team.getId();
		Long tournamentId = registration.getTournament() == null ? null : registration.getTournament().getId();

		long memberCount = playerRepository.countByTeamId(teamId);
		List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId);

		model.addAttribute("registrationId", registrationId);
		model.addAttribute("tournamentId", tournamentId);
		model.addAttribute("submittedAt", formatDate(registration.getCreatedAt()));
		model.addAttribute("statusLabel", displayRegistrationStatus(registration.getStatus()));
		model.addAttribute("statusClass", registration.getStatus() == RegistrationStatus.APPROVED ? "badge--approved" : (registration.getStatus() == RegistrationStatus.REJECTED ? "badge--rejected" : "badge--pending"));
		model.addAttribute("canApproveOrReject", registration.getStatus() == RegistrationStatus.PENDING);

		model.addAttribute("teamName", team.getName());
		model.addAttribute("captainName", team.getCaptain() == null ? "Chưa cập nhật" : (team.getCaptain().getFullName() == null ? "Chưa cập nhật" : team.getCaptain().getFullName()));
		model.addAttribute("captainPhone", team.getCaptain() == null || team.getCaptain().getPhone() == null ? "Chưa cập nhật" : team.getCaptain().getPhone());
		model.addAttribute("teamLogoUrl", team.getLogoUrl());
		model.addAttribute("createdAt", formatDate(team.getCreatedAt()));
		model.addAttribute("memberCount", memberCount);
		model.addAttribute("members", members);
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
		List<Match> matches = matchService.listByTournamentIdWithDetails(id);
		List<Match> knockoutMatches = new ArrayList<>();
		for (Match m : matches) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim();
			if (rn.toLowerCase().startsWith("bảng")) continue; // skip group
			knockoutMatches.add(m);
		}

		List<Match> semisRaw = new ArrayList<>();
		Match finalRaw = null;
		Match thirdRaw = null;
		for (Match m : knockoutMatches) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim();
			if ("Bán kết".equalsIgnoreCase(rn)) semisRaw.add(m);
			if ("Chung kết".equalsIgnoreCase(rn)) finalRaw = m;
			if (rn.toLowerCase().contains("hạng 3")) thirdRaw = m;
		}
		semisRaw.sort(Comparator.comparing(Match::getId));

		Map<String, String> seedLabels = new HashMap<>();
		for (Match m : semisRaw) {
			String homeName = m.getHomeTeam() != null ? m.getHomeTeam().getName() : null;
			String awayName = m.getAwayTeam() != null ? m.getAwayTeam().getName() : null;
			if (homeName != null && !homeName.isBlank() && !seedLabels.containsKey(homeName)) {
				seedLabels.put(homeName, "T" + (seedLabels.size() + 1));
			}
			if (awayName != null && !awayName.isBlank() && !seedLabels.containsKey(awayName)) {
				seedLabels.put(awayName, "T" + (seedLabels.size() + 1));
			}
		}

		List<BracketMatch> bracketSemis = new ArrayList<>();
		for (Match m : semisRaw) {
			String homeName = m.getHomeTeam() != null ? m.getHomeTeam().getName() : "Đội 1";
			String awayName = m.getAwayTeam() != null ? m.getAwayTeam().getName() : "Đội 2";
			Integer hs = (m.getStatus() == MatchStatus.FINISHED) ? m.getHomeScore() : null;
			Integer as = (m.getStatus() == MatchStatus.FINISHED) ? m.getAwayScore() : null;
			bracketSemis.add(new BracketMatch(homeName, awayName, hs, as));
		}

		BracketMatch bracketFinal = null;
		if (finalRaw != null) {
			String homeName = finalRaw.getHomeTeam() != null ? finalRaw.getHomeTeam().getName() : "Đội 1";
			String awayName = finalRaw.getAwayTeam() != null ? finalRaw.getAwayTeam().getName() : "Đội 2";
			Integer hs = (finalRaw.getStatus() == MatchStatus.FINISHED) ? finalRaw.getHomeScore() : null;
			Integer as = (finalRaw.getStatus() == MatchStatus.FINISHED) ? finalRaw.getAwayScore() : null;
			bracketFinal = new BracketMatch(homeName, awayName, hs, as);
		}

		BracketMatch bracketThird = null;
		if (thirdRaw != null) {
			String homeName = thirdRaw.getHomeTeam() != null ? thirdRaw.getHomeTeam().getName() : "Đội 1";
			String awayName = thirdRaw.getAwayTeam() != null ? thirdRaw.getAwayTeam().getName() : "Đội 2";
			Integer hs = (thirdRaw.getStatus() == MatchStatus.FINISHED) ? thirdRaw.getHomeScore() : null;
			Integer as = (thirdRaw.getStatus() == MatchStatus.FINISHED) ? thirdRaw.getAwayScore() : null;
			bracketThird = new BracketMatch(homeName, awayName, hs, as);
		}

		Map<String, Integer> roundPriority = new HashMap<>();
		roundPriority.put("Vòng 16", 0);
		roundPriority.put("Tứ kết", 1);
		roundPriority.put("Bán kết", 2);
		roundPriority.put("Chung kết", 3);
		roundPriority.put("Tranh hạng 3", 4);

		Map<String, List<BracketMatch>> byRound = new HashMap<>();
		for (Match m : knockoutMatches) {
			String round = m.getRoundName() == null ? "" : m.getRoundName().trim();
			if (round.isBlank()) continue;
			Integer hs = (m.getStatus() == MatchStatus.FINISHED) ? m.getHomeScore() : null;
			Integer as = (m.getStatus() == MatchStatus.FINISHED) ? m.getAwayScore() : null;
			String homeName = m.getHomeTeam() != null ? m.getHomeTeam().getName() : "Đội 1";
			String awayName = m.getAwayTeam() != null ? m.getAwayTeam().getName() : "Đội 2";
			byRound.computeIfAbsent(round, k -> new ArrayList<>())
					.add(new BracketMatch(homeName, awayName, hs, as));
		}

		// Ensure bracket stretches to the Final by adding placeholders for missing later rounds
		// Determine earliest knockout stage present
		boolean hasR16 = byRound.containsKey("Vòng 16");
		boolean hasQF = byRound.containsKey("Tứ kết");
		boolean hasSF = byRound.containsKey("Bán kết");
		boolean hasFinal = byRound.containsKey("Chung kết");

		if (hasR16) {
			int r16Count = byRound.getOrDefault("Vòng 16", List.of()).size();
			int qfExpected = Math.max(1, (int) Math.ceil(r16Count / 2.0));
			List<BracketMatch> qfList = byRound.computeIfAbsent("Tứ kết", k -> new ArrayList<>());
			if (qfList.isEmpty()) {
				for (int i = 0; i < qfExpected; i++) qfList.add(new BracketMatch("—", "—", null, null));
			}
			hasQF = true;
		}
		if (hasQF) {
			int qfCount = byRound.getOrDefault("Tứ kết", List.of()).size();
			int sfExpected = Math.max(1, (int) Math.ceil(qfCount / 2.0));
			List<BracketMatch> sfList = byRound.computeIfAbsent("Bán kết", k -> new ArrayList<>());
			if (sfList.isEmpty()) {
				for (int i = 0; i < sfExpected; i++) sfList.add(new BracketMatch("—", "—", null, null));
			}
			hasSF = true;
		}
		if (hasSF && !hasFinal) {
			byRound.put("Chung kết", new ArrayList<>(List.of(new BracketMatch("—", "—", null, null))));
		}

		List<BracketRound> rounds = new ArrayList<>();
		for (Map.Entry<String, List<BracketMatch>> e : byRound.entrySet()) {
			List<BracketMatch> ms = e.getValue();
			rounds.add(new BracketRound(e.getKey(), ms));
		}
		rounds.sort(Comparator.comparingInt(r -> roundPriority.getOrDefault(r.roundName(), 999)));

		model.addAttribute("seedLabels", seedLabels);
		model.addAttribute("bracketSemis", bracketSemis);
		model.addAttribute("bracketFinal", bracketFinal);
		model.addAttribute("bracketThird", bracketThird);
		// Use specialized SEMIS_FINAL layout only for 4-team tournaments or when there is no Quarter-final round
		boolean hasQuarterFinal = byRound.containsKey("Tứ kết");
		boolean isFourTeamTournament = tournament.getTeamLimit() != null && tournament.getTeamLimit() <= 4;
		String bracketLayout = (!hasQuarterFinal && bracketSemis.size() == 2) || isFourTeamTournament ? "SEMIS_FINAL" : "GENERIC";
		model.addAttribute("bracketLayout", bracketLayout);
		model.addAttribute("bracketRounds", rounds);
		return "admin/tournament/tournament-bracket";
	}

	@GetMapping({"/admin/manage/user-detail"})
	public String manageUserDetail(
			@RequestParam(value = "userId", required = false) Long userId,
			Model model,
			RedirectAttributes redirectAttributes
	) {
		model.addAttribute("userId", userId);
		if (userId == null) {
			return "redirect:/admin/manage/user";
		}

		AppUser user = userService.findById(userId).orElse(null);
		if (user == null) {
			return "redirect:/admin/manage/user";
		}

		var team = teamService.findCaptainTeam(userId).orElse(null);
		String teamName = team == null ? null : team.getName();

		model.addAttribute("user", user);
		model.addAttribute("registeredAt", formatDate(user.getCreatedAt()));
		model.addAttribute("roleLabel", displayUserRole(user.getRole()));
		model.addAttribute("statusLabel", displayUserStatus(user.getStatus()));
		model.addAttribute("isLocked", user.getStatus() == UserStatus.LOCKED);
		model.addAttribute("teamName", teamName == null || teamName.isBlank() ? "Chưa có đội" : teamName);
		model.addAttribute("hasTeam", teamName != null && !teamName.isBlank());
		return "admin/manage/user-detail";
	}

	@PostMapping("/admin/manage/user/toggle-lock")
	public String toggleUserLock(
			@RequestParam(value = "userId", required = false) Long userId,
			RedirectAttributes redirectAttributes
	) {
		if (userId == null) {
			return "redirect:/admin/manage/user";
		}
		AppUser user = userService.findById(userId).orElse(null);
		if (user == null) {
			return "redirect:/admin/manage/user";
		}
		UserStatus next = user.getStatus() == UserStatus.LOCKED ? UserStatus.ACTIVE : UserStatus.LOCKED;
		userService.updateStatus(userId, next);
		return "redirect:/admin/manage/user-detail?userId=" + userId;
	}

	@GetMapping({"/admin/manage/user-team-detail"})
	public String manageUserTeamDetail(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "teamId", required = false) Long teamId,
			Model model
	) {
		model.addAttribute("userId", userId);

		List<Team> teams = List.of();
		if (userId != null) {
			teams = teamService.listByCaptainWithCaptain(userId);
		}
		model.addAttribute("teams", teams);

		Team team = null;
		if (teamId != null) {
			team = teamService.findByIdWithCaptain(teamId).orElse(null);
		} else if (userId != null) {
			team = teams.isEmpty() ? null : teams.get(0);
		}
		model.addAttribute("selectedTeamId", team == null ? null : team.getId());

		if (team == null || team.getId() == null) {
			model.addAttribute("teamName", "Chưa có đội");
			model.addAttribute("teamStatus", "—");
			model.addAttribute("captainName", "—");
			model.addAttribute("createdAt", "—");
			model.addAttribute("memberCount", 0);
			model.addAttribute("tournamentCount", 0);
			model.addAttribute("teamLogoUrl", null);
			model.addAttribute("members", List.of());
			return "admin/manage/user-team-detail";
		}

		String captainName = team.getCaptain() == null ? null : team.getCaptain().getFullName();
		long memberCount = playerRepository.countByTeamId(team.getId());
		List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId());

		long tournamentCount = 0;
		var approvedRegs = tournamentRegistrationService.listApprovedByTeamId(team.getId());
		if (approvedRegs != null && !approvedRegs.isEmpty()) {
			var seenTournamentIds = new HashSet<Long>();
			for (var r : approvedRegs) {
				if (r == null || r.getTournament() == null || r.getTournament().getId() == null) continue;
				seenTournamentIds.add(r.getTournament().getId());
			}
			tournamentCount = seenTournamentIds.size();
		}

		model.addAttribute("teamName", team.getName());
		model.addAttribute("teamStatus", "Đang hoạt động");
		model.addAttribute("captainName", captainName == null || captainName.isBlank() ? "Chưa cập nhật" : captainName);
		model.addAttribute("createdAt", formatDate(team.getCreatedAt()));
		model.addAttribute("memberCount", memberCount);
		model.addAttribute("tournamentCount", tournamentCount);
		model.addAttribute("teamLogoUrl", team.getLogoUrl());
		model.addAttribute("members", members);
		return "admin/manage/user-team-detail";
	}

	@GetMapping({"/admin/manage/user-transaction-history"})
	public String manageUserTransactionHistory(
			@RequestParam(value = "userId", required = false) Long userId,
			Model model
	) {
		model.addAttribute("userId", userId);
		if (userId == null) {
			model.addAttribute("transactions", List.of());
			return "admin/manage/user-transaction-history";
		}

		List<AdminTransactionRow> rows = transactionService.listByUserId(userId).stream()
				.map(t -> {
					String code = t == null ? null : t.getCode();
					String description = t == null ? null : t.getDescription();
					String amountText = formatMoney(t == null ? null : t.getAmount());
					String timeText = formatDateTime(t == null ? null : t.getCreatedAt());
					TransactionStatus status = t == null ? null : t.getStatus();
					String statusLabel = transactionStatusLabel(status);
					String statusClass = transactionStatusClass(status);
					return new AdminTransactionRow(code, description, amountText, timeText, statusLabel, statusClass);
				})
				.toList();

		model.addAttribute("transactions", rows);
		return "admin/manage/user-transaction-history";
	}

	private String displayUserRole(UserRole role) {
		if (role == null) return "—";
		return switch (role) {
			case ADMIN -> "Admin";
			case USER -> "User";
		};
	}

	private String displayUserStatus(UserStatus status) {
		if (status == null) return "—";
		return switch (status) {
			case ACTIVE -> "Đang kích hoạt";
			case LOCKED -> "Đã khóa";
		};
	}

	private String formatDateTime(java.time.Instant instant) {
		if (instant == null) return null;
		return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
				.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
	}

	private String formatMoney(java.math.BigDecimal amount) {
		if (amount == null) return null;
		NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
		nf.setMaximumFractionDigits(0);
		nf.setMinimumFractionDigits(0);
		return nf.format(amount) + " đ";
	}

	private String transactionStatusLabel(TransactionStatus status) {
		if (status == null) return "Đang chờ";
		return switch (status) {
			case SUCCESS -> "Thành công";
			case FAILED -> "Thất bại";
			case PENDING -> "Đang chờ";
		};
	}

	private String transactionStatusClass(TransactionStatus status) {
		if (status == null) return "status--pending";
		return switch (status) {
			case SUCCESS -> "status--success";
			case FAILED -> "status--failed";
			case PENDING -> "status--pending";
		};
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
		model.addAttribute("teams", paged.getItems());
		model.addAttribute("currentPage", paged.getCurrentPage());
		model.addAttribute("pageSize", paged.getPageSize());
		model.addAttribute("totalPages", paged.getTotalPages());
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
		model.addAttribute("tournamentStartDate", tournament.getStartDate());
		model.addAttribute("tournamentEndDate", tournament.getEndDate());
		List<Match> allMatches = updateMatchStatusesByTime(matchService.listByTournamentIdWithDetails(id));
		List<Match> knockoutOnly = new ArrayList<>();
		for (Match m : allMatches) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim().toLowerCase();
			if (rn.startsWith("bảng")) continue;
			knockoutOnly.add(m);
		}

		List<Match> knockoutSource = tournament.getMode() == TournamentMode.KNOCKOUT ? allMatches : knockoutOnly;
		PagedResult<Match> paged = paginate(knockoutSource, page, size);
		model.addAttribute("knockoutMatches", paged.getItems());
		model.addAttribute("currentPage", paged.getCurrentPage());
		model.addAttribute("pageSize", paged.getPageSize());
		model.addAttribute("totalPages", paged.getTotalPages());
		model.addAttribute("pairingLocked", !allMatches.isEmpty());

		if (tournament.getMode() == TournamentMode.GROUP_STAGE) {
			Map<String, List<GroupTeamRow>> groupTeams = new HashMap<>();
			groupTeams.put("A", new ArrayList<>());
			groupTeams.put("B", new ArrayList<>());
			groupTeams.put("C", new ArrayList<>());
			groupTeams.put("D", new ArrayList<>());

			List<TournamentRegistration> registrations = tournamentRegistrationService.listApprovedByTournamentIdWithTeam(id);
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
						matchesForGroups = updateMatchStatusesByTime(matchService.listByTournamentIdWithDetails(id));
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
						List<Match> refreshed = updateMatchStatusesByTime(matchService.listByTournamentIdWithDetails(id));
						List<Match> refreshedKnockout = new ArrayList<>();
						for (Match m : refreshed) {
							if (m == null || m.getRoundName() == null) continue;
							String rn = m.getRoundName().trim().toLowerCase();
							if (rn.startsWith("bảng")) continue;
							refreshedKnockout.add(m);
						}
						PagedResult<Match> p2 = paginate(refreshedKnockout, page, size);
						model.addAttribute("knockoutMatches", p2.getItems());
						model.addAttribute("currentPage", p2.getCurrentPage());
						model.addAttribute("pageSize", p2.getPageSize());
						model.addAttribute("totalPages", p2.getTotalPages());
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

		List<TournamentRegistration> registrations = tournamentRegistrationService.listApprovedByTournamentIdWithTeam(tournamentId);
		List<Team> teams = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;

			if (registration.getStatus() != RegistrationStatus.APPROVED) continue;

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

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return "redirect:/admin/match-history?id=" + tournamentId;
		}

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

		// Guard: require score saved before finishing the match
		if (match.getHomeScore() == null || match.getAwayScore() == null) {
			return "redirect:/admin/match-history?id=" + tournamentId + "&matchId=" + matchId + "&tab=lineup&page=" + page + "&size=" + size + "&saved=score_required";
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

	public static final class AdminTransactionRow {
		private final String code;
		private final String description;
		private final String amountText;
		private final String timeText;
		private final String statusLabel;
		private final String statusClass;

		public AdminTransactionRow(String code, String description, String amountText, String timeText, String statusLabel, String statusClass) {
			this.code = code;
			this.description = description;
			this.amountText = amountText;
			this.timeText = timeText;
			this.statusLabel = statusLabel;
			this.statusClass = statusClass;
		}

		public String getCode() { return code; }
		public String getDescription() { return description; }
		public String getAmountText() { return amountText; }
		public String getTimeText() { return timeText; }
		public String getStatusLabel() { return statusLabel; }
		public String getStatusClass() { return statusClass; }
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

	public record BracketMatch(String homeTeamName, String awayTeamName, Integer homeScore, Integer awayScore) {}
	public record BracketRound(String roundName, List<BracketMatch> matches) {}

	private String displayMode(TournamentMode mode) {
		if (mode == null) return "";
		return mode == TournamentMode.GROUP_STAGE ? "Chia bảng đấu (Group Stage)" : "Knockout";
	}
	
	private String displayRegistrationStatus(RegistrationStatus status) {
		if (status == null) return "Không xác định";
		return switch (status) {
			case PENDING -> "Chờ duyệt";
			case APPROVED -> "Đã duyệt";
			case REJECTED -> "Đã hủy";
		};
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

	private List<TeamRegistrationRow> buildTeamRegistrationRows(Long tournamentId, RegistrationStatus statusFilter, String search) {
		if (tournamentId == null) {
			return List.of();
		}
		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<TeamRegistrationRow> rows = new ArrayList<>();
		String query = (search == null) ? "" : search.toLowerCase().trim();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;
			if (statusFilter != null && registration.getStatus() != statusFilter) continue;
			if (registration.getTeam() == null || registration.getTeam().getId() == null) continue;

			var captain = registration.getTeam().getCaptain();
			String representative = captain == null || captain.getFullName() == null ? "Chưa cập nhật" : captain.getFullName();
			String phone = captain == null || captain.getPhone() == null ? "Chưa cập nhật" : captain.getPhone();
			String teamName = registration.getTeam().getName();

			// Apply search filter
			if (!query.isEmpty()) {
				boolean matches = (teamName != null && teamName.toLowerCase().contains(query)) ||
						(representative != null && representative.toLowerCase().contains(query)) ||
						(phone != null && phone.toLowerCase().contains(query));
				if (!matches) continue;
			}

			long memberCount = playerRepository.countByTeamId(registration.getTeam().getId());
			rows.add(new TeamRegistrationRow(
					registration.getId(),
					formatDate(registration.getCreatedAt()),
					teamName,
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
		List<TournamentRegistration> registrations = tournamentRegistrationService.listApprovedByTournamentIdWithTeam(tournamentId);
		List<TeamListItem> teams = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;

			if (registration.getStatus() != RegistrationStatus.APPROVED) continue;
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

	public static final class AdminTopbarMessage {
		private final Long id;
		private final String name;
		private final String email;
		private final String message;
		private final String timeText;

		public AdminTopbarMessage(Long id, String name, String email, String message, String timeText) {
			this.id = id;
			this.name = name;
			this.email = email;
			this.message = message;
			this.timeText = timeText;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getEmail() {
			return email;
		}

		public String getMessage() {
			return message;
		}

		public String getTimeText() {
			return timeText;
		}
	}

	public static final class AdminTopbarNotification {
		private final String title;
		private final String detail;
		private final String timeText;
		private final String href;

		public AdminTopbarNotification(String title, String detail, String timeText, String href) {
			this.title = title;
			this.detail = detail;
			this.timeText = timeText;
			this.href = href;
		}

		public String getTitle() {
			return title;
		}

		public String getDetail() {
			return detail;
		}

		public String getTimeText() {
			return timeText;
		}

		public String getHref() {
			return href;
		}
	}

	public static final class AdminTopbarMessagesResponse {
		private final long unreadCount;
		private final List<AdminTopbarMessage> items;

		public AdminTopbarMessagesResponse(long unreadCount, List<AdminTopbarMessage> items) {
			this.unreadCount = unreadCount;
			this.items = items;
		}

		public long getUnreadCount() {
			return unreadCount;
		}

		public List<AdminTopbarMessage> getItems() {
			return items;
		}
	}

	public static final class AdminTopbarNotificationsResponse {
		private final long pendingCount;
		private final List<AdminTopbarNotification> items;

		public AdminTopbarNotificationsResponse(long pendingCount, List<AdminTopbarNotification> items) {
			this.pendingCount = pendingCount;
			this.items = items;
		}

		public long getPendingCount() {
			return pendingCount;
		}

		public List<AdminTopbarNotification> getItems() {
			return items;
		}
	}

	public static final class AdminTournamentRow {
		private final Long id;
		private final String name;
		private final String organizer;
		private final String modeLabel;
		private final String teamCountText;
		private final String statusLabel;
		private final String statusClass;
		private final String mode;
		private final String pitchType;
		private final Integer teamLimit;
		private final String startDate;
		private final String endDate;
		private final String description;

		public AdminTournamentRow(Long id, String name, String organizer, String modeLabel, String teamCountText, String statusLabel, String statusClass, String mode, String pitchType, Integer teamLimit, String startDate, String endDate, String description) {
			this.id = id;
			this.name = name;
			this.organizer = organizer;
			this.modeLabel = modeLabel;
			this.teamCountText = teamCountText;
			this.statusLabel = statusLabel;
			this.statusClass = statusClass;
			this.mode = mode;
			this.pitchType = pitchType;
			this.teamLimit = teamLimit;
			this.startDate = startDate;
			this.endDate = endDate;
			this.description = description;
		}

		public Long getId() { return id; }
		public String getName() { return name; }
		public String getOrganizer() { return organizer; }
		public String getModeLabel() { return modeLabel; }
		public String getTeamCountText() { return teamCountText; }
		public String getStatusLabel() { return statusLabel; }
		public String getStatusClass() { return statusClass; }
		public String getMode() { return mode; }
		public String getPitchType() { return pitchType; }
		public Integer getTeamLimit() { return teamLimit; }
		public String getStartDate() { return startDate; }
		public String getEndDate() { return endDate; }
		public String getDescription() { return description; }
	}

}

