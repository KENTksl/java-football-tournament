package com.example.football_tourament_web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

	@GetMapping({"/", "/admin/general-overview"})
	public String generalOverview() {
		return "/admin/general-overview";
	}
}

