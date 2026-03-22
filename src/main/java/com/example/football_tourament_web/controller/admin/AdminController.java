package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.service.MatchService;
import com.example.football_tourament_web.service.TeamService;
import com.example.football_tourament_web.service.TournamentService;
import com.example.football_tourament_web.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class AdminController {

	private final TournamentService tournamentService;
	private final TeamService teamService;
	private final MatchService matchService;
	private final UserService userService;

	public AdminController(TournamentService tournamentService, TeamService teamService, MatchService matchService, UserService userService) {
		this.tournamentService = tournamentService;
		this.teamService = teamService;
		this.matchService = matchService;
		this.userService = userService;
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
	public String adminProfile(Model model) {
		AppUser admin = userService.findByEmail("admin@example.com").orElse(null);
		model.addAttribute("admin", admin);
		return "admin/profile/admin-profile";
	}

	@PostMapping("/admin/profile/save")
	public String saveAdminProfile(@RequestParam("fullName") String fullName,
								   @RequestParam("phone") String phone,
								   @RequestParam("address") String address,
								   @RequestParam(value = "dob", required = false) String dob,
								   @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) {
		AppUser admin = userService.findByEmail("admin@example.com").orElse(null);
		if (admin != null) {
			admin.setFullName(fullName);
			admin.setPhone(phone);
			admin.setAddress(address);
			if (dob != null && !dob.isEmpty()) {
				admin.setDateOfBirth(java.time.LocalDate.parse(dob));
			}

			if (avatarFile != null && !avatarFile.isEmpty()) {
				try {
					String uploadDir = "src/main/resources/static/uploads/avatars/";
					String fileName = UUID.randomUUID().toString() + "_" + avatarFile.getOriginalFilename();
					Path uploadPath = Paths.get(uploadDir);

					if (!Files.exists(uploadPath)) {
						Files.createDirectories(uploadPath);
					}

					try (var inputStream = avatarFile.getInputStream()) {
						Path filePath = uploadPath.resolve(fileName);
						Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
						admin.setAvatar("/uploads/avatars/" + fileName);
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
	public String manageTournament() {
		return "admin/manage/manage-tournament";
	}

	@GetMapping({"/admin/manage-user", "/admin/manage/user"})
	public String manageUser() {
		return "admin/manage/manage-user";
	}

	@GetMapping("/admin/manage")
	public String manageHome() {
		return "redirect:/admin/manage/tournament";
	}

	@GetMapping("/admin/team-management")
	public String teamManagement() {
		return "admin/team/team-management";
	}

	@GetMapping("/admin/team-detail")
	public String teamDetail() {
		return "admin/team/team-detail";
	}

	@GetMapping("/admin/invoice-management")
	public String invoiceManagement() {
		return "admin/invoice/invoice-management";
	}

	@GetMapping("/admin/tournament-bracket")
	public String tournamentBracket() {
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
	public String adminInformation(Model model) {
		model.addAttribute("activePage", "info");
		return "admin/tournament/general-information";
	}

	@GetMapping("/admin/team-list")
	public String adminTeamList(Model model) {
		model.addAttribute("activePage", "teams");
		return "admin/tournament/team-list";
	}

	@GetMapping("/admin/match-history")
	public String adminMatchHistory(Model model) {
		model.addAttribute("activePage", "matches");
		return "admin/tournament/match-history";
	}

}

