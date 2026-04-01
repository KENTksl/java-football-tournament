package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTopbarService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.example.football_tourament_web.controller.admin")
public class AdminTopbarAdvice {
	private final AdminTopbarService adminTopbarService;

	public AdminTopbarAdvice(AdminTopbarService adminTopbarService) {
		this.adminTopbarService = adminTopbarService;
	}

	@ModelAttribute
	public void attachAdminTopbarModel(Model model) {
		var topbar = adminTopbarService.buildModel();
		model.addAttribute("adminTopMessages", topbar.messages());
		model.addAttribute("adminUnreadMessageCount", topbar.unreadMessageCount());
		model.addAttribute("adminTopNotifications", topbar.notifications());
		model.addAttribute("adminPendingRegistrationCount", topbar.pendingRegistrationCount());
	}
}

