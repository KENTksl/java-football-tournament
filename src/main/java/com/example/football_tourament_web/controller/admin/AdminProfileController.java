package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Controller
public class AdminProfileController {
	private final AdminProfileService adminProfileService;

	public AdminProfileController(AdminProfileService adminProfileService) {
		this.adminProfileService = adminProfileService;
	}

	@GetMapping({"/admin/admin-profile", "/admin/profile"})
	public String adminProfile(Model model, Principal principal) {
		model.addAttribute("admin", adminProfileService.findAdmin(principal));
		return "admin/profile/admin-profile";
	}

	@PostMapping("/admin/profile/save")
	public String saveAdminProfile(
			Principal principal,
			@RequestParam("fullName") String fullName,
			@RequestParam("phone") String phone,
			@RequestParam("address") String address,
			@RequestParam(value = "dob", required = false) String dob,
			@RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile
	) {
		adminProfileService.saveAdminProfile(principal, fullName, phone, address, dob, avatarFile);
		return "redirect:/admin/profile";
	}
}

