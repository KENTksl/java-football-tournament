package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserAuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserAuthController {
	private final UserAuthService userAuthService;

	public UserAuthController(UserAuthService userAuthService) {
		this.userAuthService = userAuthService;
	}

	@GetMapping({"/dang-nhap", "/dang-nhap.html"})
	public String login() {
		return "user/auth/login";
	}

	@GetMapping({"/dang-ky", "/dang-ky.html"})
	public String register(Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new UserAuthService.RegisterForm());
		}
		return "user/auth/register";
	}

	@PostMapping("/dang-ky")
	public String registerSubmit(@Valid UserAuthService.RegisterForm form, BindingResult bindingResult, Model model) {
		return userAuthService.handleRegister(form, bindingResult, model);
	}
}

