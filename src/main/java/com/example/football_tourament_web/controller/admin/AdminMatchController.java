package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.model.enums.MatchEventType;
import com.example.football_tourament_web.model.enums.TeamSide;
import com.example.football_tourament_web.service.admin.AdminMatchHistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AdminMatchController {
	private final AdminMatchHistoryService adminMatchHistoryService;

	public AdminMatchController(AdminMatchHistoryService adminMatchHistoryService) {
		this.adminMatchHistoryService = adminMatchHistoryService;
	}

	@GetMapping("/admin/match-history")
	public String adminMatchHistory(
			@RequestParam(value = "id", required = false) Long id,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "tab", required = false) String tab,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size,
			Model model
	) {
		var result = adminMatchHistoryService.buildMatchHistoryPage(id, matchId, tab, page, size);
		model.addAllAttributes(result.model());
		return result.viewName();
	}

	@PostMapping("/admin/match-history/random-groups")
	public String randomGroups(@RequestParam(value = "tournamentId", required = false) Long tournamentId) {
		return adminMatchHistoryService.randomGroups(tournamentId);
	}

	@GetMapping("/admin/match-history/match-players")
	@ResponseBody
	public AdminMatchHistoryService.MatchPlayersResponse matchPlayers(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId
	) {
		return adminMatchHistoryService.matchPlayers(tournamentId, matchId);
	}

	@PostMapping("/admin/match-history/save-lineup")
	@ResponseBody
	public void saveLineup(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "lineupJson", required = false) String lineupJson
	) {
		adminMatchHistoryService.saveLineupJson(tournamentId, matchId, lineupJson);
	}

	@PostMapping("/admin/match-history/save-events")
	@ResponseBody
	public void saveEvents(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "eventsJson", required = false) String eventsJson
	) {
		adminMatchHistoryService.saveEventsJson(tournamentId, matchId, eventsJson);
	}

	@PostMapping("/admin/match-history/random-pair")
	public String randomPairMatchHistory(@RequestParam(value = "tournamentId", required = false) Long tournamentId) {
		return adminMatchHistoryService.randomPair(tournamentId);
	}

	@PostMapping("/admin/match-history/save-schedule")
	public String saveMatchSchedule(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "time", required = false) String time,
			@RequestParam(value = "location", required = false) String location,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		return adminMatchHistoryService.saveSchedule(tournamentId, matchId, date, time, location, page, size);
	}

	@PostMapping("/admin/match-history/save-score")
	public String saveMatchScore(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "homeScore", required = false) Integer homeScore,
			@RequestParam(value = "awayScore", required = false) Integer awayScore,
			@RequestParam(value = "homePen", required = false) Integer homePen,
			@RequestParam(value = "awayPen", required = false) Integer awayPen,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		return adminMatchHistoryService.saveScore(tournamentId, matchId, homeScore, awayScore, homePen, awayPen, page, size);
	}

	@PostMapping("/admin/match-history/save-lineup-form")
	public String saveMatchLineup(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam Map<String, String> params,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		return adminMatchHistoryService.saveLineupForm(tournamentId, matchId, params, page, size);
	}

	@PostMapping("/admin/match-history/add-event")
	public String addMatchEvent(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "teamSide", required = false) TeamSide teamSide,
			@RequestParam(value = "minute", required = false) Integer minute,
			@RequestParam(value = "type", required = false) MatchEventType type,
			@RequestParam(value = "playerId", required = false) Long playerId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		return adminMatchHistoryService.addEvent(tournamentId, matchId, teamSide, minute, type, playerId, page, size);
	}

	@PostMapping("/admin/match-history/delete-event")
	public String deleteMatchEvent(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "eventId", required = false) Long eventId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		return adminMatchHistoryService.deleteEvent(tournamentId, matchId, eventId, page, size);
	}

	@PostMapping("/admin/match-history/sync-score-from-events")
	public String syncScoreFromEvents(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		return adminMatchHistoryService.syncScoreFromEvents(tournamentId, matchId, page, size);
	}

	@PostMapping("/admin/match-history/finish-match")
	public String finishMatch(
			@RequestParam(value = "tournamentId", required = false) Long tournamentId,
			@RequestParam(value = "matchId", required = false) Long matchId,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") int size
	) {
		return adminMatchHistoryService.finishMatch(tournamentId, matchId, page, size);
	}
}

