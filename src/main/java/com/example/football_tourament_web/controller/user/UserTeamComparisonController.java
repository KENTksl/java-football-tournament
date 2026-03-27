package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.model.dto.comparison.TeamComparisonDto;
import com.example.football_tourament_web.service.core.TeamComparisonService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/user/tournament/compare-teams")
@AllArgsConstructor
public class UserTeamComparisonController {

    private final TeamComparisonService teamComparisonService;

    @GetMapping
    public String getTeamComparison(
            @RequestParam("tournamentId") Long tournamentId,
            @RequestParam("team1Id") Long team1Id,
            @RequestParam("team2Id") Long team2Id,
            Model model) {

        TeamComparisonDto comparisonDto = teamComparisonService.getComparison(team1Id, team2Id);
        model.addAttribute("comparison", comparisonDto);

        return "user/tournament/team-comparison";
    }

    @GetMapping("/fragment")
    public String getTeamComparisonFragment(
            @RequestParam("tournamentId") Long tournamentId,
            @RequestParam("team1Id") Long team1Id,
            @RequestParam("team2Id") Long team2Id,
            Model model) {

        TeamComparisonDto comparisonDto = teamComparisonService.getComparison(team1Id, team2Id);
        model.addAttribute("comparison", comparisonDto);

        return "user/tournament/team-comparison :: comparison-result";
    }
}
