package com.example.football_tourament_web.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "match_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"match_id", "user_id"})
})
public class MatchVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public MatchVote() {
    }

    public MatchVote(Match match, AppUser user, Player player) {
        this.match = match;
        this.user = user;
        this.player = player;
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

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
