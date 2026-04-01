package com.example.football_tourament_web.controller.common;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.football_tourament_web.service.common.ImageService;
import com.example.football_tourament_web.service.core.UserService;

@ControllerAdvice
public class CurrentUserAdvice {
	private final UserService userService;
	private final ImageService imageService;

	public CurrentUserAdvice(UserService userService, ImageService imageService) {
		this.userService = userService;
		this.imageService = imageService;
	}

	@ModelAttribute
	public void currentUser(Model model, Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return;
		}

		Object principal = authentication.getPrincipal();
		if (principal == null || "anonymousUser".equals(principal)) {
			return;
		}

		String email = authentication.getName();
		userService.findByEmail(email).ifPresent(user -> {
			model.addAttribute("currentUserEmail", user.getEmail());
			model.addAttribute("currentUserFullName", user.getFullName());
			String avatarSrc = user.getAvatarUrl();
			if (avatarSrc == null || avatarSrc.isBlank()) {
				avatarSrc = user.getAvatar();
			}
			model.addAttribute("currentUserAvatarUrl", imageService.resolveUserAvatarUrl(avatarSrc));
		});
	}
}
