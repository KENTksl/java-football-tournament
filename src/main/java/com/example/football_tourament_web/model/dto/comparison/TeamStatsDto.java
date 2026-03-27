package com.example.football_tourament_web.model.dto.comparison;

import com.example.football_tourament_web.model.entity.Team;
import java.util.List;

public class TeamStatsDto {
    private Long id;
    private String name;
    private String logoUrl;
    private HeadToHeadStatsDto headToHeadStats;
    private List<PlayerStatsDto> topPlayers;

    public TeamStatsDto(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        this.logoUrl = team.getLogoUrl();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public HeadToHeadStatsDto getHeadToHeadStats() {
        return headToHeadStats;
    }

    public void setHeadToHeadStats(HeadToHeadStatsDto headToHeadStats) {
        this.headToHeadStats = headToHeadStats;
    }

    public List<PlayerStatsDto> getTopPlayers() {
        return topPlayers;
    }

    public void setTopPlayers(List<PlayerStatsDto> topPlayers) {
        this.topPlayers = topPlayers;
    }
}
