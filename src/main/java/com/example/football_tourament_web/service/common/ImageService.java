package com.example.football_tourament_web.service.common;

import org.springframework.stereotype.Service;

@Service
public class ImageService {
	public static final String DEFAULT_AVATAR_URL = "/img/figma-avatar.png";
	public static final String DEFAULT_TOURNAMENT_COVER_URL = "/img/home-hero.jpg";

	public String resolveUserAvatarUrl(String avatarUrl) {
		if (avatarUrl == null || avatarUrl.isBlank()) {
			return DEFAULT_AVATAR_URL;
		}
		return avatarUrl;
	}

	public String resolveTournamentCoverUrl(String coverUrl) {
		if (coverUrl == null || coverUrl.isBlank()) {
			return DEFAULT_TOURNAMENT_COVER_URL;
		}
		return coverUrl;
	}
}

