package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.service.core.MatchService;
import com.example.football_tourament_web.service.core.TeamService;
import com.example.football_tourament_web.service.core.TournamentService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminDashboardService {
	private final TournamentService tournamentService;
	private final TeamService teamService;
	private final MatchService matchService;

	public AdminDashboardService(TournamentService tournamentService, TeamService teamService, MatchService matchService) {
		this.tournamentService = tournamentService;
		this.teamService = teamService;
		this.matchService = matchService;
	}

	public Map<String, Object> buildGeneralOverviewModel() {
		Map<String, Object> model = new HashMap<>();
		model.put("totalTournaments", tournamentService.countTournaments());
		model.put("totalTeams", teamService.countTeams());
		model.put("activeTournaments", tournamentService.countTournamentsByStatus(TournamentStatus.LIVE));
		model.put("completedTournaments", tournamentService.countTournamentsByStatus(TournamentStatus.FINISHED));
		model.put("recentWinners", tournamentService.getRecentWinners());
		model.put("matchFrequency", matchService.getMatchFrequencyForLast7Months());
		return model;
	}
}
