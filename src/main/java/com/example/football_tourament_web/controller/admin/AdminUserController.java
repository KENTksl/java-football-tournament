package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminUserManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminUserController {
	private final AdminUserManagementService adminUserManagementService;

	public AdminUserController(AdminUserManagementService adminUserManagementService) {
		this.adminUserManagementService = adminUserManagementService;
	}

	@GetMapping({"/admin/manage-user", "/admin/manage/user"})
	public String manageUser(Model model) {
		model.addAttribute("users", adminUserManagementService.listUsers());
		return "admin/manage/manage-user";
	}

	@GetMapping("/admin/manage")
	public String manageHome() {
		return "redirect:/admin/manage/tournament";
	}

	@GetMapping({"/admin/manage/user-detail"})
	public String manageUserDetail(@RequestParam(value = "userId", required = false) Long userId, Model model) {
		var view = adminUserManagementService.getUserDetail(userId);
		if (view == null) return "redirect:/admin/manage/user";
		model.addAttribute("userId", userId);
		model.addAttribute("user", view.getUser());
		model.addAttribute("registeredAt", view.getRegisteredAt());
		model.addAttribute("roleLabel", view.getRoleLabel());
		model.addAttribute("statusLabel", view.getStatusLabel());
		model.addAttribute("isLocked", view.isLocked());
		model.addAttribute("teamName", view.getTeamName());
		model.addAttribute("hasTeam", view.isHasTeam());
		return "admin/manage/user-detail";
	}

	@PostMapping("/admin/manage/user/toggle-lock")
	public String toggleUserLock(@RequestParam(value = "userId", required = false) Long userId) {
		adminUserManagementService.toggleLock(userId);
		return userId == null ? "redirect:/admin/manage/user" : "redirect:/admin/manage/user-detail?userId=" + userId;
	}

	@GetMapping({"/admin/manage/user-team-detail"})
	public String manageUserTeamDetail(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "teamId", required = false) Long teamId,
			Model model
	) {
		var view = adminUserManagementService.getUserTeamDetail(userId, teamId);
		model.addAttribute("userId", view.getUserId());
		model.addAttribute("teams", view.getTeams());
		model.addAttribute("selectedTeamId", view.getSelectedTeamId());
		model.addAttribute("teamName", view.getTeamName());
		model.addAttribute("teamStatus", view.getTeamStatus());
		model.addAttribute("captainName", view.getCaptainName());
		model.addAttribute("createdAt", view.getCreatedAt());
		model.addAttribute("memberCount", view.getMemberCount());
		model.addAttribute("tournamentCount", view.getTournamentCount());
		model.addAttribute("teamLogoUrl", view.getTeamLogoUrl());
		model.addAttribute("members", view.getMembers());
		return "admin/manage/user-team-detail";
	}

	@GetMapping({"/admin/manage/user-transaction-history"})
	public String manageUserTransactionHistory(@RequestParam(value = "userId", required = false) Long userId, Model model) {
		model.addAttribute("userId", userId);
		model.addAttribute("transactions", adminUserManagementService.listUserTransactions(userId));
		return "admin/manage/user-transaction-history";
	}
}

