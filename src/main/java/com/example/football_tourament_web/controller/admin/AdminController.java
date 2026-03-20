package com.example.football_tourament_web.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

	@GetMapping({"/admin", "/admin/general-overview"})
	public String generalOverview() {
		return "admin/general-overview";
	}
	@GetMapping({"/", "/admin/admin-profile"})
	public String adminProfile() {
		return "/admin/admin-profile";
	}
}

