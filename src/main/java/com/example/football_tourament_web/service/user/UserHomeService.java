package com.example.football_tourament_web.service.user;

import com.example.football_tourament_web.model.dto.NewsItem;
import com.example.football_tourament_web.model.entity.ContactMessage;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.service.user.CbsSportsNewsService;
import com.example.football_tourament_web.service.core.ContactMessageService;
import com.example.football_tourament_web.service.common.ImageService;
import com.example.football_tourament_web.service.core.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserHomeService {
	private final CbsSportsNewsService cbsSportsNewsService;
	private final TournamentService tournamentService;
	private final ContactMessageService contactMessageService;
	private final ImageService imageService;
	private final HttpClient httpClient;

	private static final Pattern LAT_PATTERN = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern LON_PATTERN = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"");

	public UserHomeService(
			CbsSportsNewsService cbsSportsNewsService,
			TournamentService tournamentService,
			ContactMessageService contactMessageService,
			ImageService imageService
	) {
		this.cbsSportsNewsService = cbsSportsNewsService;
		this.tournamentService = tournamentService;
		this.contactMessageService = contactMessageService;
		this.imageService = imageService;
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(10))
				.build();
	}

	public HomeView buildHomeView() {
		List<FeaturedTournamentView> featuredTournaments = tournamentService.listTournaments().stream()
				.sorted((a, b) -> {
					int sa = tournamentStatusPriority(a.getStatus());
					int sb = tournamentStatusPriority(b.getStatus());
					if (sa != sb) {
						return Integer.compare(sa, sb);
					}
					if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
						return b.getCreatedAt().compareTo(a.getCreatedAt());
					}
					return Long.compare(b.getId() == null ? 0 : b.getId(), a.getId() == null ? 0 : a.getId());
				})
				.limit(3)
				.map(t -> new FeaturedTournamentView(
						t.getId(),
						t.getName(),
						imageService.resolveTournamentCoverUrl(t.getImageUrl()),
						featuredTournamentMeta(t),
						t.getStatus() == null ? "Đang cập nhật" : tournamentStatusLabel(t.getStatus())
				))
				.collect(Collectors.toList());
		return new HomeView(featuredTournaments, !featuredTournaments.isEmpty());
	}

	public NewsView buildNewsView(int page) {
		int pageSize = 6;
		List<NewsItem> allItems = cbsSportsNewsService.getHeadlines();

		int totalItems = allItems.size();
		int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
		int currentPage = Math.min(Math.max(page, 1), totalPages);

		int fromIndex = Math.min((currentPage - 1) * pageSize, totalItems);
		int toIndex = Math.min(fromIndex + pageSize, totalItems);
		List<NewsItem> pageItems = allItems.subList(fromIndex, toIndex);

		int startPage = Math.max(1, currentPage - 2);
		int endPage = Math.min(totalPages, currentPage + 2);
		if (endPage - startPage + 1 < 5) {
			endPage = Math.min(totalPages, startPage + 4);
			startPage = Math.max(1, endPage - 4);
		}

		return new NewsView(pageItems, currentPage, totalPages, startPage, endPage, !allItems.isEmpty());
	}

	public boolean submitContactMessage(String name, String email, String message) {
		String n = name == null ? "" : name.trim();
		String e = email == null ? "" : email.trim();
		String m = message == null ? "" : message.trim();

		if (n.isBlank() || e.isBlank() || m.isBlank()) {
			return false;
		}

		try {
			ContactMessage cm = new ContactMessage();
			cm.setName(n);
			cm.setEmail(e);
			cm.setMessage(m);
			contactMessageService.save(cm);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public ResponseEntity<GeocodeResult> geocode(String query) {
		if (query == null || query.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
					+ java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);

			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(10))
					.header("Accept", "application/json")
					.header("User-Agent", "football-tourament-web/1.0 (contact page map)")
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
			}

			String body = response.body();

			Matcher latMatcher = LAT_PATTERN.matcher(body);
			Matcher lonMatcher = LON_PATTERN.matcher(body);
			if (!latMatcher.find() || !lonMatcher.find()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}

			double lat = Double.parseDouble(latMatcher.group(1));
			double lon = Double.parseDouble(lonMatcher.group(1));

			Matcher displayNameMatcher = DISPLAY_NAME_PATTERN.matcher(body);
			String displayName = displayNameMatcher.find() ? decodeJsonString(displayNameMatcher.group(1)) : null;

			return ResponseEntity.ok(new GeocodeResult(lat, lon, displayName));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
		}
	}

	private static int tournamentStatusPriority(TournamentStatus status) {
		if (status == null) {
			return 10;
		}
		return switch (status) {
			case LIVE -> 0;
			case UPCOMING -> 1;
			case FINISHED -> 2;
		};
	}

	private static String featuredTournamentMeta(Tournament t) {
		if (t == null) return "";

		String teamLimit = t.getTeamLimit() == null ? null : (t.getTeamLimit() + " đội");
		String pitch = t.getPitchType() == null ? null : switch (t.getPitchType()) {
			case PITCH_5 -> "Sân 5";
			case PITCH_7 -> "Sân 7";
			case PITCH_11 -> "Sân 11";
		};
		String mode = t.getMode() == null ? null : switch (t.getMode()) {
			case KNOCKOUT -> "Knockout";
			case GROUP_STAGE -> "Vòng bảng";
		};

		List<String> parts = new ArrayList<>();
		if (teamLimit != null) parts.add(teamLimit);
		if (pitch != null) parts.add(pitch);
		if (mode != null) parts.add(mode);
		return String.join(" • ", parts);
	}

	private static String tournamentStatusLabel(TournamentStatus status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case UPCOMING -> "Sắp diễn ra";
			case LIVE -> "Đang diễn ra";
			case FINISHED -> "Đã kết thúc";
		};
	}

	private static String decodeJsonString(String input) {
		if (input == null) {
			return null;
		}
		return input
				.replace("\\\"", "\"")
				.replace("\\\\", "\\")
				.replace("\\/", "/")
				.replace("\\n", " ")
				.replace("\\r", " ")
				.replace("\\t", " ");
	}

	public record HomeView(List<FeaturedTournamentView> featuredTournaments, boolean hasFeaturedTournaments) {
	}

	public record FeaturedTournamentView(Long id, String name, String imageUrl, String meta, String statusLabel) {
	}

	public record NewsView(
			List<NewsItem> newsItems,
			int currentPage,
			int totalPages,
			int startPage,
			int endPage,
			boolean hasNews
	) {
	}

	public record GeocodeResult(double lat, double lon, String displayName) {
	}
}
