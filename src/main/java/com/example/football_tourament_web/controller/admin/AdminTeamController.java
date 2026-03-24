package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTeamManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminTeamController {
	private final AdminTeamManagementService adminTeamManagementService;

	public AdminTeamController(AdminTeamManagementService adminTeamManagementService) {
		this.adminTeamManagementService = adminTeamManagementService;
	}

	@GetMapping("/admin/team-management")
	public String teamManagement(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "page", defaultValue = "1") int page,
			Model model
	) {
		var view = adminTeamManagementService.buildTeamManagementView(tournamentId, status, search, page, 10);
		model.addAttribute("tournaments", view.tournaments());
		model.addAttribute("selectedTournamentId", view.selectedTournamentId());
		model.addAttribute("selectedStatus", view.selectedStatus());
		model.addAttribute("registrationRows", view.registrationRows());
		model.addAttribute("currentPage", view.currentPage());
		model.addAttribute("totalPages", view.totalPages());
		model.addAttribute("currentSearch", view.currentSearch());
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
		var result = adminTeamManagementService.updateRegistrationStatus(registrationId, tournamentId, targetStatus);
		redirectAttributes.addFlashAttribute("teamManageMessage", result.message());
		return "redirect:/admin/team-management?tournamentId=" + tournamentId + "&status=" + status;
	}

	@GetMapping("/admin/team-detail")
	public String teamDetail(@RequestParam(value = "id", required = false) Long registrationId, Model model) {
		var view = adminTeamManagementService.buildTeamDetailView(registrationId);
		if (view == null) return "redirect:/admin/team-management";

		model.addAttribute("registrationId", view.registrationId());
		model.addAttribute("tournamentId", view.tournamentId());
		model.addAttribute("submittedAt", view.submittedAt());
		model.addAttribute("statusLabel", view.statusLabel());
		model.addAttribute("statusClass", view.statusClass());
		model.addAttribute("canApproveOrReject", view.canApproveOrReject());
		model.addAttribute("teamName", view.teamName());
		model.addAttribute("captainName", view.captainName());
		model.addAttribute("captainPhone", view.captainPhone());
		model.addAttribute("teamLogoUrl", view.teamLogoUrl());
		model.addAttribute("createdAt", view.createdAt());
		model.addAttribute("memberCount", view.memberCount());
		model.addAttribute("members", view.members());
		return "admin/team/team-detail";
	}
}

