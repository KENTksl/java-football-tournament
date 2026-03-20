package com.example.football_tourament_web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user/tournament")
public class TournamentController {

	@GetMapping({"", "/"})
	public String tournamentList() {
		return "/user/tournament/Tournament_list";
	}

	@GetMapping("/match-schedule")
	public String matchSchedule() {
		return "/user/tournament/Match_schedule";
	}

	@GetMapping("/sign-up")
	public String signUp() {
		return "/user/tournament/Sign_up";
	}

	@GetMapping("/competing-teams")
	public String competingTeams() {
		return "/user/tournament/Competing_Teams";
	}

	@GetMapping({"/tournament", "/tournament/"})
	public String tournamentListAlias() {
		return "redirect:/user/tournament";
	}

	@GetMapping("/tournament/match-schedule")
	public String matchScheduleAlias() {
		return "redirect:/user/tournament/match-schedule";
	}

	@GetMapping("/tournament/sign-up")
	public String signUpAlias() {
		return "redirect:/user/tournament/sign-up";
	}

	@GetMapping("/tournament/competing-teams")
	public String competingTeamsAlias() {
		return "redirect:/user/tournament/competing-teams";
	}
}
