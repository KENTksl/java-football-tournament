package com.example.football_tourament_web.model.dto.comparison;

import java.time.LocalDateTime;

public class MatchHistoryDto {
    private String tournamentName;
    private LocalDateTime matchDate;
    private String score;
    private String result; // Thắng, Hòa, Thua (đối với đội 1)

    // Getters and Setters
    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public LocalDateTime getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDateTime matchDate) {
        this.matchDate = matchDate;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
