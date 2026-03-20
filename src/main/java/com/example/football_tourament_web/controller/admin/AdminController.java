package com.example.football_tourament_web.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

	@GetMapping({"/admin/manage", "/admin/user-detail", "/admin/manage/user-detail"})
	public String userDetail() {
		return "admin/manage/user-detail";
	}
}

