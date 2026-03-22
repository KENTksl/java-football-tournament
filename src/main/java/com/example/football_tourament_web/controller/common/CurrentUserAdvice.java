package com.example.football_tourament_web.controller.common;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.football_tourament_web.service.UserService;

@ControllerAdvice
public class CurrentUserAdvice {
	private final UserService userService;

	public CurrentUserAdvice(UserService userService) {
		this.userService = userService;
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
			model.addAttribute("currentUserAvatarUrl", user.getAvatarUrl());
		});
	}
}
