package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTopbarService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminTopbarController {
	private final AdminTopbarService adminTopbarService;

	public AdminTopbarController(AdminTopbarService adminTopbarService) {
		this.adminTopbarService = adminTopbarService;
	}

	@GetMapping("/admin/api/topbar/messages")
	@ResponseBody
	public ResponseEntity<AdminTopbarService.AdminTopbarMessagesResponse> topbarMessages() {
		return ResponseEntity.ok(adminTopbarService.buildMessagesResponse());
	}

	@GetMapping("/admin/api/topbar/notifications")
	@ResponseBody
	public ResponseEntity<AdminTopbarService.AdminTopbarNotificationsResponse> topbarNotifications() {
		return ResponseEntity.ok(adminTopbarService.buildNotificationsResponse());
	}
}

