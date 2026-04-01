package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UserProfileController {
	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping({"/ca-nhan", "/ca-nhan.html"})
	public String profile(Model model, Authentication authentication) {
		var user = userProfileService.requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		userProfileService.attachCommonProfileModel(model, user);
		if (!model.containsAttribute("profileForm")) {
			model.addAttribute("profileForm", UserProfileService.ProfileForm.fromUser(user));
		}
		return "user/profile/profile";
	}

	@PostMapping("/ca-nhan")
	public String updateProfile(
			@Valid UserProfileService.ProfileForm profileForm,
			BindingResult bindingResult,
			@RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
			Authentication authentication,
			Model model
	) {
		return userProfileService.handleUpdateProfile(profileForm, bindingResult, avatarFile, authentication, model);
	}

	@GetMapping({"/lich-su-dang-ky", "/lich-su-dang-ky.html"})
	public String registrationHistory(Model model, Authentication authentication) {
		var user = userProfileService.requireCurrentUser(authentication);
		if (user == null) return "redirect:/dang-nhap";

		userProfileService.attachCommonProfileModel(model, user);
		model.addAttribute("registrations", userProfileService.listRegistrations(user));
		return "user/profile/registration-history";
	}

	@GetMapping({"/lich-su-giao-dich", "/lich-su-giao-dich.html"})
	public String transactionHistory(Model model, Authentication authentication) {
		var user = userProfileService.requireCurrentUser(authentication);
		if (user == null) return "redirect:/dang-nhap";

		userProfileService.attachCommonProfileModel(model, user);
		model.addAttribute("transactions", userProfileService.listTransactions(user));
		return "user/profile/transaction-history";
	}
}

