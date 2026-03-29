package com.example.football_tourament_web.model.dto.comparison;

import java.util.List;

public class TeamComparisonDto {
    private TeamStatsDto team1Stats;
    private TeamStatsDto team2Stats;
    private List<MatchHistoryDto> matchHistory;
    private List<String> tacticalInsights;

    // Getters and Setters
    public TeamStatsDto getTeam1Stats() {
        return team1Stats;
    }

    public void setTeam1Stats(TeamStatsDto team1Stats) {
        this.team1Stats = team1Stats;
    }

    public TeamStatsDto getTeam2Stats() {
        return team2Stats;
    }

    public void setTeam2Stats(TeamStatsDto team2Stats) {
        this.team2Stats = team2Stats;
    }

    public List<MatchHistoryDto> getMatchHistory() {
        return matchHistory;
    }

    public void setMatchHistory(List<MatchHistoryDto> matchHistory) {
        this.matchHistory = matchHistory;
    }

    public List<String> getTacticalInsights() {
        return tacticalInsights;
    }

    public void setTacticalInsights(List<String> tacticalInsights) {
        this.tacticalInsights = tacticalInsights;
    }
}
