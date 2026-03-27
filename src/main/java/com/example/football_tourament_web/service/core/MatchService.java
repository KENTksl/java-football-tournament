package com.example.football_tourament_web.service.core;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.repository.MatchRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchService {
	private final MatchRepository matchRepository;
	private final TournamentRegistrationService tournamentRegistrationService;

	public MatchService(MatchRepository matchRepository, TournamentRegistrationService tournamentRegistrationService) {
		this.matchRepository = matchRepository;
		this.tournamentRegistrationService = tournamentRegistrationService;
	}

	@Transactional(readOnly = true)
	public List<Match> listByTournamentId(Long tournamentId) {
		return matchRepository.findByTournamentIdOrderByScheduledAtAsc(tournamentId);
	}

	@Transactional(readOnly = true)
	public List<Match> listByTournamentIdWithDetails(Long tournamentId) {
		return matchRepository.findByTournamentIdWithDetails(tournamentId);
	}

	@Transactional(readOnly = true)
	public long countByTournamentId(Long tournamentId) {
		if (tournamentId == null) return 0;
		return matchRepository.countByTournamentId(tournamentId);
	}

	@Transactional(readOnly = true)
	public Optional<Match> findById(Long id) {
		return matchRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public Optional<Match> findByIdWithDetails(Long id) {
		return matchRepository.findByIdWithDetails(id);
	}

	@Transactional
	public Match save(Match match) {
		return matchRepository.save(match);
	}

	@Transactional
	public List<Match> saveAll(List<Match> matches) {
		return matchRepository.saveAll(matches);
	}

	@Transactional
	public boolean generateNextKnockoutRoundIfReady(Long tournamentId, String currentRoundName) {
		if (tournamentId == null) return false;
		if (currentRoundName == null || currentRoundName.isBlank()) return false;
		String currentRound = currentRoundName.trim();
		if (currentRound.toLowerCase().startsWith("bảng")) return false;

		List<Match> allMatches = matchRepository.findByTournamentIdWithDetails(tournamentId);
		if (allMatches == null || allMatches.isEmpty()) return false;

		Tournament tournament = allMatches.get(0).getTournament();
		if (tournament == null) return false;
		if (tournament.getMode() != TournamentMode.KNOCKOUT && tournament.getMode() != TournamentMode.GROUP_STAGE) return false;

		String nextRoundName = nextRoundName(currentRound, tournament.getTeamLimit());
		if (nextRoundName == null) return false;

		boolean nextRoundExists = allMatches.stream().anyMatch(m -> {
			if (m == null || m.getRoundName() == null) return false;
			return nextRoundName.equalsIgnoreCase(m.getRoundName().trim());
		});
		if (nextRoundExists) return false;

		List<Match> currentRoundMatches = allMatches.stream()
				.filter(m -> m != null && m.getRoundName() != null && currentRound.equalsIgnoreCase(m.getRoundName().trim()))
				.sorted(Comparator.comparing(Match::getId))
				.collect(Collectors.toList());
		if (currentRoundMatches.isEmpty()) return false;

		boolean allFinished = currentRoundMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
		if (!allFinished) return false;

		List<Team> winners = new ArrayList<>();
		for (Match m : currentRoundMatches) {
			Team winner = winnerOf(m);
			if (winner == null) {
				return false;
			}
			winners.add(winner);
		}
		if (winners.size() < 2 || winners.size() % 2 != 0) return false;

		List<Match> nextRoundMatches = new ArrayList<>();
		for (int i = 0; i < winners.size(); i += 2) {
			Team home = winners.get(i);
			Team away = winners.get(i + 1);
			Match nextMatch = new Match(tournament, home, away);
			nextMatch.setRoundName(nextRoundName);
			nextMatch.setStatus(MatchStatus.SCHEDULED);
			nextRoundMatches.add(nextMatch);
		}
		assignKnockoutTimes(tournament, currentRoundMatches, nextRoundName, nextRoundMatches);
		matchRepository.saveAll(nextRoundMatches);
		return true;
	}

	@Transactional
	public boolean generateGroupStageMatchesIfMissing(Tournament tournament, List<TournamentRegistration> registrations) {
		if (tournament == null || tournament.getId() == null) return false;
		if (tournament.getMode() != TournamentMode.GROUP_STAGE) return false;
		if (tournament.getTeamLimit() == null || tournament.getTeamLimit() != 16) return false;
		if (registrations == null || registrations.isEmpty()) return false;

		List<Match> existing = matchRepository.findByTournamentIdWithDetails(tournament.getId());
		boolean alreadyGenerated = existing.stream().anyMatch(m -> m.getRoundName() != null && m.getRoundName().startsWith("Bảng "));
		if (alreadyGenerated) return false;

		List<TournamentRegistration> regs = registrations.stream()
				.filter(r -> r != null && r.getTeam() != null && r.getTeam().getId() != null)
				.collect(Collectors.toList());

		List<Team> groupA = new ArrayList<>();
		List<Team> groupB = new ArrayList<>();
		List<Team> groupC = new ArrayList<>();
		List<Team> groupD = new ArrayList<>();

		for (TournamentRegistration r : regs) {
			String g = r.getGroupName() == null ? "" : r.getGroupName().trim().toUpperCase();
			if ("A".equals(g)) groupA.add(r.getTeam());
			if ("B".equals(g)) groupB.add(r.getTeam());
			if ("C".equals(g)) groupC.add(r.getTeam());
			if ("D".equals(g)) groupD.add(r.getTeam());
		}

		if (groupA.size() != 4 || groupB.size() != 4 || groupC.size() != 4 || groupD.size() != 4) return false;

		List<Match> matches = new ArrayList<>();
		matches.addAll(buildRoundRobinMatches(tournament, "Bảng A", groupA));
		matches.addAll(buildRoundRobinMatches(tournament, "Bảng B", groupB));
		matches.addAll(buildRoundRobinMatches(tournament, "Bảng C", groupC));
		matches.addAll(buildRoundRobinMatches(tournament, "Bảng D", groupD));

		matchRepository.saveAll(matches);
		return true;
	}

	@Transactional
	public boolean generateQuarterFinalsFromGroupsIfReady(Long tournamentId) {
		if (tournamentId == null) return false;
		List<Match> allMatches = matchRepository.findByTournamentIdWithDetails(tournamentId);
		if (allMatches == null || allMatches.isEmpty()) return false;

		Tournament tournament = allMatches.get(0).getTournament();
		if (tournament == null || tournament.getMode() != TournamentMode.GROUP_STAGE) return false;
		if (tournament.getTeamLimit() == null || tournament.getTeamLimit() != 16) return false;

		boolean knockoutAlreadyExists = allMatches.stream().anyMatch(m -> {
			if (m == null || m.getRoundName() == null) return false;
			String rn = m.getRoundName().trim();
			return "Tứ kết".equalsIgnoreCase(rn) || "Bán kết".equalsIgnoreCase(rn) || "Chung kết".equalsIgnoreCase(rn);
		});
		if (knockoutAlreadyExists) return false;

		List<TournamentRegistration> regs = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId).stream()
				.filter(r -> r != null
						&& r.getStatus() == RegistrationStatus.APPROVED
						&& r.getTeam() != null
						&& r.getTeam().getId() != null)
				.collect(Collectors.toList());

		Map<String, List<Team>> teamsByGroup = new HashMap<>();
		teamsByGroup.put("A", new ArrayList<>());
		teamsByGroup.put("B", new ArrayList<>());
		teamsByGroup.put("C", new ArrayList<>());
		teamsByGroup.put("D", new ArrayList<>());

		for (TournamentRegistration r : regs) {
			String g = r.getGroupName() == null ? "" : r.getGroupName().trim().toUpperCase();
			if (!teamsByGroup.containsKey(g)) continue;
			teamsByGroup.get(g).add(r.getTeam());
		}
		if (teamsByGroup.get("A").size() != 4 || teamsByGroup.get("B").size() != 4 || teamsByGroup.get("C").size() != 4 || teamsByGroup.get("D").size() != 4) {
			return false;
		}

		Map<String, Map<Long, GroupStats>> stats = new HashMap<>();
		for (String g : teamsByGroup.keySet()) {
			Map<Long, GroupStats> m = new HashMap<>();
			for (Team t : teamsByGroup.get(g)) {
				m.put(t.getId(), new GroupStats());
			}
			stats.put(g, m);
		}

		List<Match> groupMatches = allMatches.stream()
				.filter(m -> m != null && m.getRoundName() != null && m.getRoundName().trim().toLowerCase().startsWith("bảng "))
				.collect(Collectors.toList());
		if (groupMatches.isEmpty()) return false;

		for (Match m : groupMatches) {
			if (m.getRoundName() == null) continue;
			String rn = m.getRoundName().trim();
			String g = null;
			if ("Bảng A".equalsIgnoreCase(rn)) g = "A";
			if ("Bảng B".equalsIgnoreCase(rn)) g = "B";
			if ("Bảng C".equalsIgnoreCase(rn)) g = "C";
			if ("Bảng D".equalsIgnoreCase(rn)) g = "D";
			if (g == null) continue;

			if (m.getStatus() != MatchStatus.FINISHED) return false;
			Integer hs = m.getHomeScore();
			Integer as = m.getAwayScore();
			if (hs == null || as == null) return false;
			if (m.getHomeTeam() == null || m.getAwayTeam() == null) return false;
			Long hid = m.getHomeTeam().getId();
			Long aid = m.getAwayTeam().getId();
			if (hid == null || aid == null) return false;

			GroupStats home = stats.get(g).get(hid);
			GroupStats away = stats.get(g).get(aid);
			if (home == null || away == null) continue;

			home.gf += hs;
			home.ga += as;
			away.gf += as;
			away.ga += hs;

			if (hs > as) {
				home.points += 3;
			} else if (as > hs) {
				away.points += 3;
			} else {
				home.points += 1;
				away.points += 1;
			}
		}

		Comparator<Team> comparator = (t1, t2) -> {
			GroupStats s1 = stats.getOrDefault(groupOfTeam(teamsByGroup, t1), Map.of()).get(t1.getId());
			GroupStats s2 = stats.getOrDefault(groupOfTeam(teamsByGroup, t2), Map.of()).get(t2.getId());
			int p1 = s1 == null ? 0 : s1.points;
			int p2 = s2 == null ? 0 : s2.points;
			if (p1 != p2) return Integer.compare(p2, p1);
			int gd1 = s1 == null ? 0 : (s1.gf - s1.ga);
			int gd2 = s2 == null ? 0 : (s2.gf - s2.ga);
			if (gd1 != gd2) return Integer.compare(gd2, gd1);
			int gf1 = s1 == null ? 0 : s1.gf;
			int gf2 = s2 == null ? 0 : s2.gf;
			if (gf1 != gf2) return Integer.compare(gf2, gf1);
			String n1 = t1.getName() == null ? "" : t1.getName();
			String n2 = t2.getName() == null ? "" : t2.getName();
			return n1.compareToIgnoreCase(n2);
		};

		List<Team> a = new ArrayList<>(teamsByGroup.get("A"));
		List<Team> b = new ArrayList<>(teamsByGroup.get("B"));
		List<Team> c = new ArrayList<>(teamsByGroup.get("C"));
		List<Team> d = new ArrayList<>(teamsByGroup.get("D"));
		a.sort(comparator);
		b.sort(comparator);
		c.sort(comparator);
		d.sort(comparator);

		Team a1 = a.get(0);
		Team a2 = a.get(1);
		Team b1 = b.get(0);
		Team b2 = b.get(1);
		Team c1 = c.get(0);
		Team c2 = c.get(1);
		Team d1 = d.get(0);
		Team d2 = d.get(1);

		List<Match> quarterFinals = new ArrayList<>();
		quarterFinals.add(buildKnockoutMatch(tournament, "Tứ kết", a1, b2));
		quarterFinals.add(buildKnockoutMatch(tournament, "Tứ kết", a2, b1));
		quarterFinals.add(buildKnockoutMatch(tournament, "Tứ kết", c1, d2));
		quarterFinals.add(buildKnockoutMatch(tournament, "Tứ kết", c2, d1));
		assignQuarterFinalTimes(tournament, groupMatches, quarterFinals);
		matchRepository.saveAll(quarterFinals);
		return true;
	}

	private static Match buildKnockoutMatch(Tournament tournament, String roundName, Team home, Team away) {
		Match m = new Match(tournament, home, away);
		m.setRoundName(roundName);
		m.setStatus(MatchStatus.SCHEDULED);
		return m;
	}

	private static String groupOfTeam(Map<String, List<Team>> teamsByGroup, Team team) {
		if (team == null || team.getId() == null) return "";
		for (Map.Entry<String, List<Team>> e : teamsByGroup.entrySet()) {
			for (Team t : e.getValue()) {
				if (t != null && team.getId().equals(t.getId())) return e.getKey();
			}
		}
		return "";
	}

	private static final class GroupStats {
		int points;
		int gf;
		int ga;
	}

	private static List<Match> buildRoundRobinMatches(Tournament tournament, String roundName, List<Team> teams) {
		List<Match> matches = new ArrayList<>();
		if (teams == null || teams.size() != 4) return matches;
		Team t0 = teams.get(0);
		Team t1 = teams.get(1);
		Team t2 = teams.get(2);
		Team t3 = teams.get(3);

		int roundGapDays = 2;
		LocalDate start = tournament.getStartDate() != null ? tournament.getStartDate() : LocalDate.now().plusDays(1);
		LocalDateTime base = start.atTime(18, 0);

		// Lượt 1
		matches.add(buildScheduledGroupMatch(tournament, roundName, t0, t1, base.plusHours(0)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t2, t3, base.plusHours(2)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t0, t2, base.plusDays(roundGapDays).plusHours(0)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t1, t3, base.plusDays(roundGapDays).plusHours(2)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t0, t3, base.plusDays(roundGapDays * 2L).plusHours(0)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t1, t2, base.plusDays(roundGapDays * 2L).plusHours(2)));

		// Lượt 2
		LocalDateTime baseLeg2 = base.plusDays(roundGapDays * 3L);
		matches.add(buildScheduledGroupMatch(tournament, roundName, t1, t0, baseLeg2.plusHours(0)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t3, t2, baseLeg2.plusHours(2)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t2, t0, baseLeg2.plusDays(roundGapDays).plusHours(0)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t3, t1, baseLeg2.plusDays(roundGapDays).plusHours(2)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t3, t0, baseLeg2.plusDays(roundGapDays * 2L).plusHours(0)));
		matches.add(buildScheduledGroupMatch(tournament, roundName, t2, t1, baseLeg2.plusDays(roundGapDays * 2L).plusHours(2)));

		return matches;
	}

	private static Match buildScheduledGroupMatch(Tournament t, String roundName, Team home, Team away, LocalDateTime when) {
		Match m = new Match(t, home, away);
		m.setRoundName(roundName);
		m.setStatus(MatchStatus.SCHEDULED);
		m.setScheduledAt(when);
		return m;
	}

	private static LocalDateTime latestScheduledAt(List<Match> matches) {
		LocalDateTime max = null;
		for (Match m : matches) {
			if (m == null) continue;
			LocalDateTime s = m.getScheduledAt();
			if (s == null) continue;
			if (max == null || s.isAfter(max)) max = s;
		}
		return max;
	}

	private void assignQuarterFinalTimes(Tournament tournament, List<Match> groupMatches, List<Match> qfs) {
		if (qfs == null || qfs.isEmpty()) return;
		LocalDateTime base = latestScheduledAt(groupMatches);
		if (base == null) {
			LocalDate start = tournament.getStartDate() != null ? tournament.getStartDate() : LocalDate.now().plusDays(1);
			base = start.atTime(18, 0);
		} else {
			base = base.plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
		}
		for (int i = 0; i < qfs.size(); i++) {
			int dayOffset = i < 2 ? 0 : 1;
			int hourOffset = (i % 2 == 0) ? 0 : 2;
			qfs.get(i).setScheduledAt(base.plusDays(dayOffset).plusHours(hourOffset));
		}
	}

	private void assignKnockoutTimes(Tournament tournament, List<Match> prevRoundMatches, String nextRoundName, List<Match> nextMatches) {
		if (nextMatches == null || nextMatches.isEmpty()) return;
		LocalDateTime base = latestScheduledAt(prevRoundMatches);
		if (base == null) {
			LocalDate start = tournament.getStartDate() != null ? tournament.getStartDate() : LocalDate.now().plusDays(1);
			base = start.atTime(18, 0);
		} else {
			base = base.plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0);
		}
		if ("Bán kết".equalsIgnoreCase(nextRoundName)) {
			for (int i = 0; i < nextMatches.size(); i++) {
				int hourOffset = (i % 2 == 0) ? 0 : 2;
				nextMatches.get(i).setScheduledAt(base.plusHours(hourOffset));
			}
		} else if ("Chung kết".equalsIgnoreCase(nextRoundName)) {
			nextMatches.get(0).setScheduledAt(base.plusDays(2).plusHours(1));
		} else {
			for (int i = 0; i < nextMatches.size(); i++) {
				int dayOffset = i < 2 ? 0 : 1;
				int hourOffset = (i % 2 == 0) ? 0 : 2;
				nextMatches.get(i).setScheduledAt(base.plusDays(dayOffset).plusHours(hourOffset));
			}
		}
	}

	public Team winnerOf(Match match) {
		if (match == null) return null;
		if (match.getHomeTeam() == null || match.getAwayTeam() == null) return null;
		Integer homeScore = match.getHomeScore();
		Integer awayScore = match.getAwayScore();
		if (homeScore == null || awayScore == null) return null;
		if (homeScore.equals(awayScore)) {
			Integer hp = match.getHomePenalty();
			Integer ap = match.getAwayPenalty();
			if (hp == null || ap == null) return null;
			if (hp.equals(ap)) return null;
			return hp > ap ? match.getHomeTeam() : match.getAwayTeam();
		}
		return homeScore > awayScore ? match.getHomeTeam() : match.getAwayTeam();
	}

	private static String nextRoundName(String currentRoundName, Integer teamLimit) {
		if (currentRoundName == null) return null;
		String r = currentRoundName.trim();
		if (teamLimit != null && teamLimit == 16 && "Vòng 16".equals(r)) return "Tứ kết";
		if (teamLimit != null && teamLimit == 8 && "Tứ kết".equals(r)) return "Bán kết";
		if (teamLimit != null && teamLimit == 4 && "Bán kết".equals(r)) return "Chung kết";
		if ("Tứ kết".equals(r)) return "Bán kết";
		if ("Bán kết".equals(r)) return "Chung kết";
		return null;
	}

	@Transactional(readOnly = true)
	public List<Long> getMatchFrequencyForLast7Months() {
		List<Long> frequency = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();

		for (int i = 6; i >= 0; i--) {
			YearMonth ym = YearMonth.from(now).minusMonths(i);
			LocalDateTime start = ym.atDay(1).atStartOfDay();
			LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

			Instant startInstant = start.atZone(ZoneId.systemDefault()).toInstant();
			Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();

			frequency.add(matchRepository.countMatchesByScheduleOrCreation(start, end, startInstant, endInstant));
		}
		return frequency;
	}
}

