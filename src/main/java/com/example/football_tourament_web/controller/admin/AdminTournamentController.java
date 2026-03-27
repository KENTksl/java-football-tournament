package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTournamentViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class AdminTournamentController {
	private final AdminTournamentViewService adminTournamentViewService;

	public AdminTournamentController(AdminTournamentViewService adminTournamentViewService) {
		this.adminTournamentViewService = adminTournamentViewService;
	}

	@GetMapping("/admin/tournament-bracket")
	public String tournamentBracket(@RequestParam(value = "id", required = false) Long id, Model model) {
		var page = adminTournamentViewService.buildTournamentBracketPage(id);
		model.addAllAttributes(page.model());
		return page.viewName();
	}

	@GetMapping("/admin/best-players")
	public String bestPlayers(@RequestParam(value = "id", required = false) Long id, Model model) {
		var page = adminTournamentViewService.buildBestPlayersPage(id);
		model.addAllAttributes(page.model());
		return page.viewName();
	}

	@GetMapping("/admin/general-information")
	public String adminInformation(@RequestParam(value = "id", required = false) Long id, Model model) {
		var page = adminTournamentViewService.buildGeneralInformationPage(id);
		model.addAllAttributes(page.model());
		return page.viewName();
	}

	@GetMapping("/admin/team-list")
	public String adminTeamList(
			@RequestParam(value = "id", required = false) Long id,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size,
			Model model
	) {
		var result = adminTournamentViewService.buildTeamListPage(id, page, size);
		model.addAllAttributes(result.model());
		return result.viewName();
	}

	@GetMapping("/admin/team-list/team-players")
	@ResponseBody
	public List<AdminTournamentViewService.PlayerDto> teamPlayers(@RequestParam(value = "teamId", required = false) Long teamId) {
		return adminTournamentViewService.listTeamPlayers(teamId);
	}
}

