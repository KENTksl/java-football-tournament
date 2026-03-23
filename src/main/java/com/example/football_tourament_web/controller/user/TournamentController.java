package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.TournamentService;
import com.example.football_tourament_web.service.UserTournamentViewService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/tournament")
public class TournamentController {

	private final TournamentService tournamentService;
	private final UserTournamentViewService userTournamentViewService;

	public TournamentController(TournamentService tournamentService, UserTournamentViewService userTournamentViewService) {
		this.tournamentService = tournamentService;
		this.userTournamentViewService = userTournamentViewService;
	}

	@GetMapping({"", "/"})
	public String tournamentList(Model model) {
		var tournaments = tournamentService.listTournaments();
		model.addAttribute("tournaments", tournaments);
		model.addAttribute("registeredTeamCounts", userTournamentViewService.buildRegisteredTeamCountMap(tournaments));
		model.addAttribute("registeredTeamPercents", userTournamentViewService.buildRegisteredTeamPercentMap(tournaments));
		return "user/tournament/Tournament_list";
	}

	@GetMapping("/match-schedule")
	public String matchSchedule(@RequestParam(value = "id", required = false) Long id, Model model) {
		attachTournament(model, id);
		attachScheduleView(model, id);
		return "user/tournament/Match_schedule";
	}

	@GetMapping("/sign-up")
	public String signUp(@RequestParam(value = "id", required = false) Long id, Authentication authentication, Model model) {
		attachTournament(model, id);
		model.addAttribute("signUpTeams", userTournamentViewService.listSignUpTeamOptions(authentication));
		return "user/tournament/Sign_up";
	}

	@PostMapping("/sign-up")
	public String signUpSubmit(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "teamId", required = false) Long teamId,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		var result = userTournamentViewService.submitRegistration(authentication, tournamentId, teamId);
		redirectAttributes.addFlashAttribute("registrationMessage", result.message());
		redirectAttributes.addFlashAttribute("registrationSuccess", result.success());
		return "redirect:/user/tournament/sign-up?id=" + (tournamentId == null ? "" : tournamentId);
	}

	@GetMapping("/sign-up/prefill")
	@ResponseBody
	public UserTournamentViewService.TeamPrefillResponse signUpPrefill(Authentication authentication) {
		return userTournamentViewService.buildTeamPrefill(authentication);
	}

	@GetMapping("/match-lineup")
	@ResponseBody
	public UserTournamentViewService.MatchLineupView matchLineup(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId
	) {
		return userTournamentViewService.buildMatchLineupView(tournamentId, matchId);
	}

	@GetMapping("/team-lineup")
	@ResponseBody
	public UserTournamentViewService.TeamSingleLineupView teamLineup(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "teamId", required = false) Long teamId
	) {
		return userTournamentViewService.buildTeamLineupView(tournamentId, teamId);
	}

	@GetMapping("/competing-teams")
	public String competingTeams(@RequestParam(value = "id", required = false) Long id, Model model) {
		attachTournament(model, id);
		return "user/tournament/Competing_Teams";
	}

	@GetMapping("/standing-fragment")
	public String standingFragment(@RequestParam(value = "id", required = false) Long id, Model model) {
		attachTournament(model, id);
		attachScheduleView(model, id);
		return "user/tournament/fragments/standing :: standing";
	}

	@GetMapping("/match-schedule-fragment")
	public String matchScheduleFragment() {
		return "user/tournament/Match_schedule :: match-schedule";
	}

	@GetMapping("/statistics-fragment")
	public String statisticsFragment(@RequestParam(value = "id", required = false) Long id, Model model) {
		attachTournament(model, id);
		model.addAttribute("stats", userTournamentViewService.buildTournamentStats(id));
		return "user/tournament/fragments/statistics :: statistics";
	}

	@GetMapping("/charts-fragment")
	public String chartsFragment(
			@RequestParam(value = "id") Long id,
			@RequestParam(value = "teamId", required = false) Long teamId,
			Model model
	) {
		attachTournament(model, id);
		model.addAttribute("charts", userTournamentViewService.buildChartsData(id, teamId));
		model.addAttribute("teams", userTournamentViewService.buildTournamentTeams(id));
		model.addAttribute("selectedTeamId", teamId != null ? teamId.toString() : "");
		return "user/tournament/fragments/charts :: charts";
	}

	@GetMapping("/bracket-fragment")
	public String bracketFragment(@RequestParam(value = "id", required = false) Long id, Model model) {
		attachTournament(model, id);
		model.addAttribute("bracket", userTournamentViewService.buildBracketData(id));
		return "user/tournament/fragments/bracket :: bracket";
	}

	@GetMapping("/teams-fragment")
	public String teamsFragment(@RequestParam(value = "id", required = false) Long id, Model model) {
		attachTournament(model, id);
		model.addAttribute("teamCards", userTournamentViewService.buildTournamentTeams(id));
		return "user/tournament/fragments/teams :: teams";
	}

	private void attachTournament(Model model, Long id) {
		var tournament = userTournamentViewService.findTournamentOrNull(id);
		model.addAttribute("tournament", tournament);
		if (tournament != null && tournament.getId() != null) {
			model.addAttribute("registeredTeamCount", userTournamentViewService.countRegisteredTeams(tournament.getId()));
			model.addAttribute("tournamentIsFull", userTournamentViewService.isTournamentFull(tournament));
		} else {
			model.addAttribute("registeredTeamCount", 0L);
			model.addAttribute("tournamentIsFull", false);
		}
	}

	private void attachScheduleView(Model model, Long tournamentId) {
		var schedule = userTournamentViewService.buildScheduleView(tournamentId);
		model.addAttribute("scheduleReady", schedule.scheduleReady());
		model.addAttribute("scheduleMode", schedule.scheduleMode());
		model.addAttribute("scheduleMessage", schedule.scheduleMessage());
		model.addAttribute("groupingLocked", schedule.groupingLocked());
		model.addAttribute("groupTeams", schedule.groupTeams());
		model.addAttribute("groupMatches", schedule.groupMatches());
		model.addAttribute("knockoutMatches", schedule.knockoutMatches());
	}
}
