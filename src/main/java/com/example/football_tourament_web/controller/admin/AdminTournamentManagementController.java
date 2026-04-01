package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTournamentManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AdminTournamentManagementController {
	private final AdminTournamentManagementService adminTournamentManagementService;

	public AdminTournamentManagementController(AdminTournamentManagementService adminTournamentManagementService) {
		this.adminTournamentManagementService = adminTournamentManagementService;
	}

	@GetMapping({"/admin/manage-tournament", "/admin/manage/tournament"})
	public String manageTournament(Model model) {
		var tournaments = adminTournamentManagementService.listTournaments();
		var registeredTeamCounts = adminTournamentManagementService.buildRegisteredTeamCountMap(tournaments);
		model.addAttribute("tournaments", tournaments);
		model.addAttribute("registeredTeamCounts", registeredTeamCounts);
		model.addAttribute("tournamentRows", adminTournamentManagementService.buildTournamentRows(tournaments, registeredTeamCounts));
		return "admin/manage/manage-tournament";
	}

	@GetMapping("/admin/manage/tournament/add")
	public String addTournamentPage() {
		return "admin/manage/add-tournament";
	}

	@PostMapping("/admin/manage/tournament/add")
	public String addTournament(
			@RequestParam("name") String name,
			@RequestParam("organizer") String organizer,
			@RequestParam("mode") String mode,
			@RequestParam("pitchType") String pitchType,
			@RequestParam("teams") String teams,
			@RequestParam(value = "registrationFee", required = false) String registrationFee,
			@RequestParam("startDate") String startDate,
			@RequestParam("endDate") String endDate,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "image", required = false) MultipartFile image
	) {
		adminTournamentManagementService.addTournament(name, organizer, mode, pitchType, teams, registrationFee, startDate, endDate, description, image);
		return "redirect:/admin/manage/tournament";
	}

	@GetMapping("/admin/manage/tournament/edit")
	public String editTournamentPage(@RequestParam("id") Long id, Model model) {
		model.addAttribute("tournament", adminTournamentManagementService.findById(id));
		return "admin/manage/edit-tournament";
	}

	@GetMapping("/admin/manage/tournament/edit/{id}")
	public String editTournamentPageByPath(@PathVariable("id") Long id, Model model) {
		model.addAttribute("tournament", adminTournamentManagementService.findById(id));
		return "admin/manage/edit-tournament";
	}

	@PostMapping("/admin/manage/tournament/edit")
	public String editTournament(
			@RequestParam("id") Long id,
			@RequestParam("name") String name,
			@RequestParam("organizer") String organizer,
			@RequestParam("mode") String mode,
			@RequestParam("pitchType") String pitchType,
			@RequestParam("teams") String teams,
			@RequestParam(value = "registrationFee", required = false) String registrationFee,
			@RequestParam("startDate") String startDate,
			@RequestParam("endDate") String endDate,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "image", required = false) MultipartFile image
	) {
		adminTournamentManagementService.editTournament(id, name, organizer, mode, pitchType, teams, registrationFee, startDate, endDate, description, image);
		return "redirect:/admin/manage/tournament";
	}

	@PostMapping("/admin/manage/tournament/edit/{id}")
	public String editTournamentByPath(
			@PathVariable("id") Long id,
			@RequestParam("name") String name,
			@RequestParam("organizer") String organizer,
			@RequestParam("mode") String mode,
			@RequestParam("pitchType") String pitchType,
			@RequestParam("teams") String teams,
			@RequestParam(value = "registrationFee", required = false) String registrationFee,
			@RequestParam("startDate") String startDate,
			@RequestParam("endDate") String endDate,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "image", required = false) MultipartFile image
	) {
		adminTournamentManagementService.editTournament(id, name, organizer, mode, pitchType, teams, registrationFee, startDate, endDate, description, image);
		return "redirect:/admin/manage/tournament";
	}

	@PostMapping("/admin/manage/tournament/delete")
	public String deleteTournament(@RequestParam("id") Long id) {
		adminTournamentManagementService.deleteTournament(id);
		return "redirect:/admin/manage/tournament";
	}

	@PostMapping("/admin/manage/tournament/delete/{id}")
	public String deleteTournamentByPath(@PathVariable("id") Long id) {
		adminTournamentManagementService.deleteTournament(id);
		return "redirect:/admin/manage/tournament";
	}
}
