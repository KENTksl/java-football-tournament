package com.example.football_tourament_web.service.core;

import com.example.football_tourament_web.model.dto.comparison.HeadToHeadStatsDto;
import com.example.football_tourament_web.model.dto.comparison.MatchHistoryDto;
import com.example.football_tourament_web.model.dto.comparison.PlayerStatsDto;
import com.example.football_tourament_web.model.dto.comparison.TeamComparisonDto;
import com.example.football_tourament_web.model.dto.comparison.TeamStatsDto;
import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.enums.MatchEventType;
import com.example.football_tourament_web.repository.MatchEventRepository;
import com.example.football_tourament_web.repository.MatchRepository;
import com.example.football_tourament_web.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamComparisonService {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;

    public TeamComparisonService(TeamRepository teamRepository, MatchRepository matchRepository, MatchEventRepository matchEventRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.matchEventRepository = matchEventRepository;
    }

    @Transactional(readOnly = true)
    public TeamComparisonDto getComparison(Long team1Id, Long team2Id) {
        Team team1 = teamRepository.findById(team1Id).orElseThrow(() -> new RuntimeException("Team not found"));
        Team team2 = teamRepository.findById(team2Id).orElseThrow(() -> new RuntimeException("Team not found"));

        TeamComparisonDto comparisonDto = new TeamComparisonDto();
        comparisonDto.setTeam1Stats(getTeamStats(team1, team2));
        comparisonDto.setTeam2Stats(getTeamStats(team2, team1));
        comparisonDto.setMatchHistory(getMatchHistory(team1, team2));
        comparisonDto.setTacticalInsights(generateTacticalInsights(comparisonDto.getTeam1Stats(), comparisonDto.getTeam2Stats()));

        return comparisonDto;
    }

    private List<MatchHistoryDto> getMatchHistory(Team team1, Team team2) {
        List<Match> matches = matchRepository.findMatchesBetweenTeams(team1.getId(), team2.getId());
        return matches.stream()
                .map(match -> {
                    MatchHistoryDto dto = new MatchHistoryDto();
                    dto.setTournamentName(match.getTournament().getName());
                    dto.setMatchDate(match.getScheduledAt());
                    dto.setScore(match.getHomeScore() + " - " + match.getAwayScore());
                    
                    boolean isTeam1Home = match.getHomeTeam().getId().equals(team1.getId());
                    if (match.getHomeScore() > match.getAwayScore()) {
                        dto.setResult(isTeam1Home ? "Thắng" : "Thua");
                    } else if (match.getHomeScore() < match.getAwayScore()) {
                        dto.setResult(isTeam1Home ? "Thua" : "Thắng");
                    } else {
                        dto.setResult("Hòa");
                    }
                    return dto;
                })
                .sorted(Comparator.comparing(MatchHistoryDto::getMatchDate).reversed())
                .collect(Collectors.toList());
    }

    private TeamStatsDto getTeamStats(Team team, Team opponent) {
        TeamStatsDto teamStatsDto = new TeamStatsDto(team);
        teamStatsDto.setHeadToHeadStats(getHeadToHeadStats(team, opponent));
        teamStatsDto.setTopPlayers(getTopPlayerStats(team));
        return teamStatsDto;
    }

    private HeadToHeadStatsDto getHeadToHeadStats(Team team, Team opponent) {
        List<Match> matches = matchRepository.findMatchesBetweenTeams(team.getId(), opponent.getId());
        HeadToHeadStatsDto stats = new HeadToHeadStatsDto();

        for (Match match : matches) {
            boolean isHomeTeam = match.getHomeTeam().getId().equals(team.getId());
            if (match.getHomeScore() > match.getAwayScore()) {
                if (isHomeTeam) stats.setWins(stats.getWins() + 1);
                else stats.setLosses(stats.getLosses() + 1);
            } else if (match.getHomeScore() < match.getAwayScore()) {
                if (isHomeTeam) stats.setLosses(stats.getLosses() + 1);
                else stats.setWins(stats.getWins() + 1);
            } else {
                stats.setDraws(stats.getDraws() + 1);
            }
            stats.setGoalsFor(stats.getGoalsFor() + (isHomeTeam ? match.getHomeScore() : match.getAwayScore()));
            stats.setGoalsAgainst(stats.getGoalsAgainst() + (isHomeTeam ? match.getAwayScore() : match.getHomeScore()));
        }
        return stats;
    }

    private List<PlayerStatsDto> getTopPlayerStats(Team team) {
        return team.getPlayers().stream()
                .map(this::getPlayerStats)
                .sorted(Comparator.comparingInt(PlayerStatsDto::getGoals).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private PlayerStatsDto getPlayerStats(Player player) {
        PlayerStatsDto stats = new PlayerStatsDto(player);
        stats.setGoals((int) matchEventRepository.countByPlayerAndType(player, MatchEventType.GOAL));
        stats.setYellowCards((int) matchEventRepository.countByPlayerAndType(player, MatchEventType.YELLOW));
        stats.setRedCards((int) matchEventRepository.countByPlayerAndType(player, MatchEventType.RED));
        return stats;
    }

    private List<String> generateTacticalInsights(TeamStatsDto team1Stats, TeamStatsDto team2Stats) {
        List<String> insights = new ArrayList<>();

        // Head-to-head insights
        if (team1Stats.getHeadToHeadStats().getWins() > team2Stats.getHeadToHeadStats().getWins()) {
            insights.add(team1Stats.getName() + " có thành tích đối đầu tốt hơn so với " + team2Stats.getName() + ".");
        } else if (team1Stats.getHeadToHeadStats().getWins() < team2Stats.getHeadToHeadStats().getWins()) {
            insights.add(team2Stats.getName() + " có thành tích đối đầu tốt hơn so với " + team1Stats.getName() + ".");
        } else {
            insights.add("Hai đội có thành tích đối đầu khá cân bằng.");
        }

        // Goal scoring insights
        if (team1Stats.getHeadToHeadStats().getGoalsFor() > team2Stats.getHeadToHeadStats().getGoalsFor()) {
            insights.add(team1Stats.getName() + " ghi được nhiều bàn thắng hơn trong các cuộc đối đầu.");
        } else {
            insights.add(team2Stats.getName() + " ghi được nhiều bàn thắng hơn trong các cuộc đối đầu.");
        }

        // Top player insights
        if (!team1Stats.getTopPlayers().isEmpty()) {
            insights.add("Cầu thủ nổi bật của " + team1Stats.getName() + " là " + team1Stats.getTopPlayers().get(0).getPlayerName() + ", người đã ghi " + team1Stats.getTopPlayers().get(0).getGoals() + " bàn.");
        }
        if (!team2Stats.getTopPlayers().isEmpty()) {
            insights.add("Hàng phòng ngự cần chú ý đến " + team2Stats.getTopPlayers().get(0).getPlayerName() + " của " + team2Stats.getName() + ".");
        }

        return insights;
    }
}
