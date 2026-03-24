package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserHomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserHomeController {
	private final UserHomeService userHomeService;

	public UserHomeController(UserHomeService userHomeService) {
		this.userHomeService = userHomeService;
	}

	@GetMapping({"/", "/home"})
	public String home(Model model) {
		var view = userHomeService.buildHomeView();
		model.addAttribute("featuredTournaments", view.featuredTournaments());
		model.addAttribute("hasFeaturedTournaments", view.hasFeaturedTournaments());
		return "user/home/index";
	}

	@GetMapping({"/tin-tuc", "/tin-tuc.html"})
	public String news(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
		var view = userHomeService.buildNewsView(page);
		model.addAttribute("newsItems", view.newsItems());
		model.addAttribute("currentPage", view.currentPage());
		model.addAttribute("totalPages", view.totalPages());
		model.addAttribute("startPage", view.startPage());
		model.addAttribute("endPage", view.endPage());
		model.addAttribute("hasNews", view.hasNews());
		return "user/home/news";
	}

	@GetMapping({"/gioi-thieu", "/gioi-thieu.html"})
	public String aboutUs() {
		return "user/home/about-us";
	}

	@GetMapping({"/lien-he", "/lien-he.html"})
	public String contact() {
		return "user/home/contact";
	}

	@PostMapping("/lien-he")
	public String submitContactMessage(
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "email", required = false) String email,
			@RequestParam(name = "message", required = false) String message
	) {
		boolean ok = userHomeService.submitContactMessage(name, email, message);
		return ok ? "redirect:/lien-he?sent" : "redirect:/lien-he?error";
	}

	@GetMapping("/api/geocode")
	@ResponseBody
	public ResponseEntity<UserHomeService.GeocodeResult> geocode(@RequestParam(name = "q") String query) {
		return userHomeService.geocode(query);
	}
}

