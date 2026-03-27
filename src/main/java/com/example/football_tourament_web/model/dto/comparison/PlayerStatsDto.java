package com.example.football_tourament_web.model.dto.comparison;

import com.example.football_tourament_web.model.entity.Player;

public class PlayerStatsDto {
    private String playerName;
    private String playerAvatar;
    private int goals;
    private int yellowCards;
    private int redCards;

    public PlayerStatsDto(Player player) {
        this.playerName = player.getFullName();
        this.playerAvatar = player.getAvatarUrl();
    }

    // Getters and Setters
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerAvatar() {
        return playerAvatar;
    }

    public void setPlayerAvatar(String playerAvatar) {
        this.playerAvatar = playerAvatar;
    }

    public int getGoals() {
        return goals;
    }

    public void setGoals(int goals) {
        this.goals = goals;
    }

    public int getYellowCards() {
        return yellowCards;
    }

    public void setYellowCards(int yellowCards) {
        this.yellowCards = yellowCards;
    }

    public int getRedCards() {
        return redCards;
    }

    public void setRedCards(int redCards) {
        this.redCards = redCards;
    }
}
