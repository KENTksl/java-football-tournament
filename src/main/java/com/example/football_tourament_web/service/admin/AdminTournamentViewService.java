package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.core.MatchService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TournamentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminTournamentViewService {
	private final TournamentService tournamentService;
	private final MatchService matchService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final PlayerRepository playerRepository;

	public AdminTournamentViewService(
			TournamentService tournamentService,
			MatchService matchService,
			TournamentRegistrationService tournamentRegistrationService,
			PlayerRepository playerRepository
	) {
		this.tournamentService = tournamentService;
		this.matchService = matchService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.playerRepository = playerRepository;
	}

	public PageResult buildTournamentBracketPage(Long tournamentId) {
		if (tournamentId == null) return PageResult.redirect("/admin/manage/tournament");

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) return PageResult.redirect("/admin/manage/tournament");

		Map<String, Object> model = new HashMap<>();
		applyTournamentContext(model, tournament);

		List<Match> matches = matchService.listByTournamentIdWithDetails(tournamentId);
		List<Match> knockoutMatches = new ArrayList<>();
		for (Match m : matches) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim();
			if (rn.toLowerCase().startsWith("bảng")) continue;
			knockoutMatches.add(m);
		}

		List<Match> semisRaw = new ArrayList<>();
		Match finalRaw = null;
		Match thirdRaw = null;
		for (Match m : knockoutMatches) {
			if (m == null || m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim();
			if ("Bán kết".equalsIgnoreCase(rn)) semisRaw.add(m);
			if ("Chung kết".equalsIgnoreCase(rn)) finalRaw = m;
			if (rn.toLowerCase().contains("hạng 3")) thirdRaw = m;
		}
		semisRaw.sort(Comparator.comparing(Match::getId));

		Map<String, String> seedLabels = new HashMap<>();
		for (Match m : semisRaw) {
			String homeName = m.getHomeTeam() != null ? m.getHomeTeam().getName() : null;
			String awayName = m.getAwayTeam() != null ? m.getAwayTeam().getName() : null;
			if (homeName != null && !homeName.isBlank() && !seedLabels.containsKey(homeName)) {
				seedLabels.put(homeName, "T" + (seedLabels.size() + 1));
			}
			if (awayName != null && !awayName.isBlank() && !seedLabels.containsKey(awayName)) {
				seedLabels.put(awayName, "T" + (seedLabels.size() + 1));
			}
		}

		List<BracketMatch> bracketSemis = new ArrayList<>();
		for (Match m : semisRaw) {
			String homeName = m.getHomeTeam() != null ? m.getHomeTeam().getName() : "Đội 1";
			String awayName = m.getAwayTeam() != null ? m.getAwayTeam().getName() : "Đội 2";
			Integer hs = (m.getStatus() == MatchStatus.FINISHED) ? m.getHomeScore() : null;
			Integer as = (m.getStatus() == MatchStatus.FINISHED) ? m.getAwayScore() : null;
			Integer hp = (m.getStatus() == MatchStatus.FINISHED) ? m.getHomePenalty() : null;
			Integer ap = (m.getStatus() == MatchStatus.FINISHED) ? m.getAwayPenalty() : null;
			bracketSemis.add(new BracketMatch(homeName, awayName, hs, as, hp, ap));
		}

		BracketMatch bracketFinal = null;
		if (finalRaw != null) {
			String homeName = finalRaw.getHomeTeam() != null ? finalRaw.getHomeTeam().getName() : "Đội 1";
			String awayName = finalRaw.getAwayTeam() != null ? finalRaw.getAwayTeam().getName() : "Đội 2";
			Integer hs = (finalRaw.getStatus() == MatchStatus.FINISHED) ? finalRaw.getHomeScore() : null;
			Integer as = (finalRaw.getStatus() == MatchStatus.FINISHED) ? finalRaw.getAwayScore() : null;
			Integer hp = (finalRaw.getStatus() == MatchStatus.FINISHED) ? finalRaw.getHomePenalty() : null;
			Integer ap = (finalRaw.getStatus() == MatchStatus.FINISHED) ? finalRaw.getAwayPenalty() : null;
			bracketFinal = new BracketMatch(homeName, awayName, hs, as, hp, ap);
		}

		BracketMatch bracketThird = null;
		if (thirdRaw != null) {
			String homeName = thirdRaw.getHomeTeam() != null ? thirdRaw.getHomeTeam().getName() : "Đội 1";
			String awayName = thirdRaw.getAwayTeam() != null ? thirdRaw.getAwayTeam().getName() : "Đội 2";
			Integer hs = (thirdRaw.getStatus() == MatchStatus.FINISHED) ? thirdRaw.getHomeScore() : null;
			Integer as = (thirdRaw.getStatus() == MatchStatus.FINISHED) ? thirdRaw.getAwayScore() : null;
			Integer hp = (thirdRaw.getStatus() == MatchStatus.FINISHED) ? thirdRaw.getHomePenalty() : null;
			Integer ap = (thirdRaw.getStatus() == MatchStatus.FINISHED) ? thirdRaw.getAwayPenalty() : null;
			bracketThird = new BracketMatch(homeName, awayName, hs, as, hp, ap);
		}

		Map<String, Integer> roundPriority = new HashMap<>();
		roundPriority.put("Vòng 16", 0);
		roundPriority.put("Tứ kết", 1);
		roundPriority.put("Bán kết", 2);
		roundPriority.put("Chung kết", 3);
		roundPriority.put("Tranh hạng 3", 4);

		Map<String, List<BracketMatch>> byRound = new HashMap<>();
		for (Match m : knockoutMatches) {
			String round = m.getRoundName() == null ? "" : m.getRoundName().trim();
			if (round.isBlank()) continue;
			Integer hs = (m.getStatus() == MatchStatus.FINISHED) ? m.getHomeScore() : null;
			Integer as = (m.getStatus() == MatchStatus.FINISHED) ? m.getAwayScore() : null;
			Integer hp = (m.getStatus() == MatchStatus.FINISHED) ? m.getHomePenalty() : null;
			Integer ap = (m.getStatus() == MatchStatus.FINISHED) ? m.getAwayPenalty() : null;
			String homeName = m.getHomeTeam() != null ? m.getHomeTeam().getName() : "Đội 1";
			String awayName = m.getAwayTeam() != null ? m.getAwayTeam().getName() : "Đội 2";
			byRound.computeIfAbsent(round, k -> new ArrayList<>())
					.add(new BracketMatch(homeName, awayName, hs, as, hp, ap));
		}

		boolean hasR16 = byRound.containsKey("Vòng 16");
		boolean hasQF = byRound.containsKey("Tứ kết");
		boolean hasSF = byRound.containsKey("Bán kết");
		boolean hasFinal = byRound.containsKey("Chung kết");

		if (hasR16) {
			int r16Count = byRound.getOrDefault("Vòng 16", List.of()).size();
			int qfExpected = Math.max(1, (int) Math.ceil(r16Count / 2.0));
			List<BracketMatch> qfList = byRound.computeIfAbsent("Tứ kết", k -> new ArrayList<>());
			if (qfList.isEmpty()) {
				for (int i = 0; i < qfExpected; i++) qfList.add(new BracketMatch("—", "—", null, null, null, null));
			}
			hasQF = true;
		}
		if (hasQF) {
			int qfCount = byRound.getOrDefault("Tứ kết", List.of()).size();
			int sfExpected = Math.max(1, (int) Math.ceil(qfCount / 2.0));
			List<BracketMatch> sfList = byRound.computeIfAbsent("Bán kết", k -> new ArrayList<>());
			if (sfList.isEmpty()) {
				for (int i = 0; i < sfExpected; i++) sfList.add(new BracketMatch("—", "—", null, null, null, null));
			}
			hasSF = true;
		}
		if (hasSF && !hasFinal) {
			byRound.put("Chung kết", new ArrayList<>(List.of(new BracketMatch("—", "—", null, null, null, null))));
		}

		List<BracketRound> rounds = new ArrayList<>();
		for (Map.Entry<String, List<BracketMatch>> e : byRound.entrySet()) {
			rounds.add(new BracketRound(e.getKey(), e.getValue()));
		}
		rounds.sort(Comparator.comparingInt(r -> roundPriority.getOrDefault(r.roundName(), 999)));

		boolean hasQuarterFinal = byRound.containsKey("Tứ kết");
		boolean isFourTeamTournament = tournament.getTeamLimit() != null && tournament.getTeamLimit() <= 4;
		String bracketLayout = (!hasQuarterFinal && bracketSemis.size() == 2) || isFourTeamTournament ? "SEMIS_FINAL" : "GENERIC";

		model.put("seedLabels", seedLabels);
		model.put("bracketSemis", bracketSemis);
		model.put("bracketFinal", bracketFinal);
		model.put("bracketThird", bracketThird);
		model.put("bracketLayout", bracketLayout);
		model.put("bracketRounds", rounds);

		return PageResult.view("admin/tournament/tournament-bracket", model);
	}

	public PageResult buildGeneralInformationPage(Long tournamentId) {
		if (tournamentId == null) return PageResult.redirect("/admin/manage/tournament");
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) return PageResult.redirect("/admin/manage/tournament");

		Map<String, Object> model = new HashMap<>();
		applyTournamentContext(model, tournament);
		model.put("organizerName", tournament.getOrganizer());
		model.put("mode", displayMode(tournament.getMode()));
		model.put("teamFormat", displayTeamFormat(tournament.getTeamLimit()));
		model.put("status", displayStatus(tournament.getStatus()));
		model.put("description", tournament.getDescription());
		return PageResult.view("admin/tournament/general-information", model);
	}

	public PageResult buildTeamListPage(Long tournamentId, int page, int size) {
		if (tournamentId == null) return PageResult.redirect("/admin/manage/tournament");
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) return PageResult.redirect("/admin/manage/tournament");

		Map<String, Object> model = new HashMap<>();
		applyTournamentContext(model, tournament);
		List<TeamListItem> allTeams = buildTeamListItems(tournamentId);
		PagedResult<TeamListItem> paged = paginate(allTeams, page, size);
		model.put("teams", paged.getItems());
		model.put("currentPage", paged.getCurrentPage());
		model.put("pageSize", paged.getPageSize());
		model.put("totalPages", paged.getTotalPages());
		return PageResult.view("admin/tournament/team-list", model);
	}

	public List<PlayerDto> listTeamPlayers(Long teamId) {
		if (teamId == null) return List.of();
		List<PlayerDto> players = new ArrayList<>();
		for (Player p : playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId)) {
			players.add(new PlayerDto(p.getId(), p.getFullName(), p.getJerseyNumber(), p.getPosition(), p.getAvatarUrl()));
		}
		return players;
	}

	private void applyTournamentContext(Map<String, Object> model, Tournament tournament) {
		model.put("tournamentId", tournament.getId());
		model.put("tournamentName", tournament.getName());
	}

	private String displayMode(TournamentMode mode) {
		if (mode == null) return "";
		return mode == TournamentMode.GROUP_STAGE ? "Chia bảng đấu (Group Stage)" : "Knockout";
	}

	private String displayTeamFormat(Integer teamLimit) {
		if (teamLimit == null) return "";
		return teamLimit + " v " + teamLimit;
	}

	private String displayStatus(TournamentStatus status) {
		if (status == null) return "";
		return switch (status) {
			case UPCOMING -> "Sắp diễn ra";
			case LIVE -> "Đang diễn ra";
			case FINISHED -> "Hoàn thành";
		};
	}

	private List<TeamListItem> buildTeamListItems(Long tournamentId) {
		List<TournamentRegistration> registrations = tournamentRegistrationService.listApprovedByTournamentIdWithTeam(tournamentId);
		List<TeamListItem> teams = new ArrayList<>();
		List<Long> seenTeamIds = new ArrayList<>();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;
			if (registration.getStatus() != RegistrationStatus.APPROVED) continue;
			if (registration.getTeam() == null || registration.getTeam().getId() == null) continue;

			Long teamId = registration.getTeam().getId();
			if (seenTeamIds.contains(teamId)) continue;
			seenTeamIds.add(teamId);

			long memberCount = playerRepository.countByTeamId(teamId);
			teams.add(new TeamListItem(teamId, registration.getTeam().getName(), memberCount));
		}
		return teams;
	}

	private static <T> PagedResult<T> paginate(List<T> items, int page, int size) {
		int safeSize = Math.max(1, size);
		int totalItems = items == null ? 0 : items.size();
		int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
		int safePage = Math.min(Math.max(1, page), totalPages);
		int fromIndex = Math.min((safePage - 1) * safeSize, totalItems);
		int toIndex = Math.min(fromIndex + safeSize, totalItems);
		List<T> pageItems = totalItems == 0 ? List.of() : items.subList(fromIndex, toIndex);
		return new PagedResult<>(pageItems, safePage, safeSize, totalPages);
	}

	public record PageResult(String viewName, Map<String, Object> model) {
		public static PageResult view(String viewName, Map<String, Object> model) {
			return new PageResult(viewName, model);
		}

		public static PageResult redirect(String to) {
			return new PageResult("redirect:" + to, Map.of());
		}
	}

	public record BracketMatch(String homeTeamName, String awayTeamName, Integer homeScore, Integer awayScore, Integer homePenalty, Integer awayPenalty) {
		public String homeDisplayScore() {
			if (homeScore == null) return "";
			if (awayScore != null && homeScore.equals(awayScore) && homePenalty != null && awayPenalty != null) {
				return homeScore + " (" + homePenalty + ")";
			}
			return String.valueOf(homeScore);
		}

		public String awayDisplayScore() {
			if (awayScore == null) return "";
			if (homeScore != null && homeScore.equals(awayScore) && homePenalty != null && awayPenalty != null) {
				return awayScore + " (" + awayPenalty + ")";
			}
			return String.valueOf(awayScore);
		}

		public boolean homeWinner() {
			if (homeScore == null || awayScore == null) return false;
			if (homeScore > awayScore) return true;
			if (awayScore > homeScore) return false;
			if (homePenalty != null && awayPenalty != null) {
				return homePenalty > awayPenalty;
			}
			return false;
		}

		public boolean awayWinner() {
			if (homeScore == null || awayScore == null) return false;
			if (awayScore > homeScore) return true;
			if (homeScore > awayScore) return false;
			if (homePenalty != null && awayPenalty != null) {
				return awayPenalty > homePenalty;
			}
			return false;
		}
	}

	public record BracketRound(String roundName, List<BracketMatch> matches) {
	}

	public record PlayerDto(Long id, String fullName, Integer jerseyNumber, String position, String avatarUrl) {
	}

	public static final class TeamListItem {
		private final Long id;
		private final String name;
		private final long memberCount;

		public TeamListItem(Long id, String name, long memberCount) {
			this.id = id;
			this.name = name;
			this.memberCount = memberCount;
		}

		public Long getId() { return id; }
		public String getName() { return name; }
		public long getMemberCount() { return memberCount; }
	}

	public static final class PagedResult<T> {
		private final List<T> items;
		private final int currentPage;
		private final int pageSize;
		private final int totalPages;

		public PagedResult(List<T> items, int currentPage, int pageSize, int totalPages) {
			this.items = items;
			this.currentPage = currentPage;
			this.pageSize = pageSize;
			this.totalPages = totalPages;
		}

		public List<T> getItems() { return items; }
		public int getCurrentPage() { return currentPage; }
		public int getPageSize() { return pageSize; }
		public int getTotalPages() { return totalPages; }
	}
}
