package com.example.football_tourament_web.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class AdminController {

	@GetMapping({"/admin", "/admin/general-overview"})
	public String generalOverview() {
		return "admin/general-overview";
	}

	@GetMapping({"/admin/admin-profile", "/admin/profile"})
	public String adminProfile() {
		return "admin/admin-profile";
	}

	@GetMapping({"/admin/manage", "/admin/manage-tournament", "/admin/manage/tournament"})
	public String manageTournament() {
		return "admin/manage/manage-tournament";
	}

	@GetMapping({"/admin/manage", "/admin/manage-user", "/admin/manage/user"})
	public String manageUser() {
		return "admin/manage/manage-user";
	}

	@GetMapping({"/admin/manage", "admin/manage/user-detail", "/admin/manage/user-detail"})
	public String manageUserDetail() {
		return "admin/manage/user-detail";
	}


	@GetMapping("/admin/general-information")
	public String adminInformation(Model model) {
		model.addAttribute("activePage", "info");
		return "admin/general-information";
	}

	@GetMapping("/admin/team-list")
	public String adminTeamList(Model model) {
		model.addAttribute("activePage", "teams");
		return "admin/team-list";
	}

	@GetMapping("/admin/match-history")
	public String adminMatchHistory(Model model) {
		model.addAttribute("activePage", "matches");
		return "admin/match-history";
	}
}

