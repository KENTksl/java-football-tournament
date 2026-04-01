package com.example.football_tourament_web.model.dto;

public record NewsItem(
		String title,
		String link,
		String description,
		String imageUrl,
		String publishedAt
) {
}
