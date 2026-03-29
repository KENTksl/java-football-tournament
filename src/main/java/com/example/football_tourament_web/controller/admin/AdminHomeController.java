package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {
	private final AdminDashboardService adminDashboardService;

	public AdminHomeController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping({"/admin", "/admin/general-overview"})
	public String generalOverview(Model model) {
		model.addAllAttributes(adminDashboardService.buildGeneralOverviewModel());
		return "admin/dashboard/general-overview";
	}
}

