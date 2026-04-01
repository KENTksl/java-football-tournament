package com.example.football_tourament_web.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "match_ratings")
public class MatchRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private Double rating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public MatchRating() {
    }

    public MatchRating(Match match, Player player, Double rating) {
        this.match = match;
        this.player = player;
        this.rating = rating;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
