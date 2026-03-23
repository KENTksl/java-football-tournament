package com.example.football_tourament_web.config;

import com.example.football_tourament_web.model.entity.*;
import com.example.football_tourament_web.model.enums.*;
import com.example.football_tourament_web.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {
	private static final String DEFAULT_PLAYER_AVATAR_URL = "/assets/figma/avatar.jpg";

	private final AppUserRepository userRepository;
	private final TeamRepository teamRepository;
	private final PlayerRepository playerRepository;
	private final TournamentRepository tournamentRepository;
	private final TournamentRegistrationRepository registrationRepository;
	private final MatchRepository matchRepository;
	private final TransactionRepository transactionRepository;
	private final PasswordEncoder passwordEncoder;

	public DataSeeder(
		AppUserRepository userRepository,
		TeamRepository teamRepository,
		PlayerRepository playerRepository,
		TournamentRepository tournamentRepository,
		TournamentRegistrationRepository registrationRepository,
		MatchRepository matchRepository,
		TransactionRepository transactionRepository,
		PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.teamRepository = teamRepository;
		this.playerRepository = playerRepository;
		this.tournamentRepository = tournamentRepository;
		this.registrationRepository = registrationRepository;
		this.matchRepository = matchRepository;
		this.transactionRepository = transactionRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		var admin = getOrCreateAdmin();
		var userA = getOrCreateUserA();

		var team1 = getOrCreateTeam("FC Test 196093", userA);
		var team2 = getOrCreateTeam("FC Test 195891", null);
		var team3 = getOrCreateTeam("FC Test 196267", null);
		var team4 = getOrCreateTeam("22DTHC3", null);

		seedPlayersIfMissing(team1, List.of(
			buildPlayer(team1, "Nguyễn Văn B", 10, "Cầu thủ", "FW"),
			buildPlayer(team1, "Trần Văn C", 7, "Cầu thủ", "MF"),
			buildPlayer(team1, "Lê Văn D", 1, "Thủ môn", "GK"),
			buildPlayer(team1, "Phạm Văn E", 9, "Cầu thủ", "FW"),
			buildPlayer(team1, "Võ Văn F", 4, "Cầu thủ", "DF"),
			buildPlayer(team1, "Ngô Văn G", 5, "Cầu thủ", "DF"),
			buildPlayer(team1, "Đặng Văn H", 11, "Cầu thủ", "FW")
		));

		seedPlayersIfMissing(team2, List.of(
			buildPlayer(team2, "Cầu thủ 1", 10, "Cầu thủ", "FW"),
			buildPlayer(team2, "Cầu thủ 2", 7, "Cầu thủ", "MF"),
			buildPlayer(team2, "Cầu thủ 3", 1, "Thủ môn", "GK"),
			buildPlayer(team2, "Cầu thủ 4", 9, "Cầu thủ", "FW"),
			buildPlayer(team2, "Cầu thủ 5", 4, "Cầu thủ", "DF"),
			buildPlayer(team2, "Cầu thủ 6", 5, "Cầu thủ", "DF"),
			buildPlayer(team2, "Cầu thủ 7", 11, "Cầu thủ", "FW")
		));

		seedPlayersIfMissing(team3, List.of(
			buildPlayer(team3, "Cầu thủ A", 10, "Cầu thủ", "FW"),
			buildPlayer(team3, "Cầu thủ B", 7, "Cầu thủ", "MF"),
			buildPlayer(team3, "Cầu thủ C", 1, "Thủ môn", "GK"),
			buildPlayer(team3, "Cầu thủ D", 9, "Cầu thủ", "FW"),
			buildPlayer(team3, "Cầu thủ E", 4, "Cầu thủ", "DF"),
			buildPlayer(team3, "Cầu thủ F", 5, "Cầu thủ", "DF"),
			buildPlayer(team3, "Cầu thủ G", 11, "Cầu thủ", "FW")
		));

		seedPlayersIfMissing(team4, List.of(
			buildPlayer(team4, "Cầu thủ X", 10, "Cầu thủ", "FW"),
			buildPlayer(team4, "Cầu thủ Y", 7, "Cầu thủ", "MF"),
			buildPlayer(team4, "Thủ môn Z", 1, "Thủ môn", "GK"),
			buildPlayer(team4, "Cầu thủ M", 9, "Cầu thủ", "FW"),
			buildPlayer(team4, "Cầu thủ N", 4, "Cầu thủ", "DF"),
			buildPlayer(team4, "Cầu thủ P", 5, "Cầu thủ", "DF"),
			buildPlayer(team4, "Cầu thủ Q", 11, "Cầu thủ", "FW")
		));

		var tournament31 = getOrCreateTournament31();
		var tournament32 = getOrCreateTournament32();
		var tournament34 = getOrCreateTournament34Knockout16();
		var tournamentEpl16 = getOrCreateTournamentEplGroup16Pitch5();
		var tournamentLaLiga8 = getOrCreateTournamentLaLigaKnockout8();

		seedRegistrationIfMissing(tournament31, team1, userA, RegistrationStatus.APPROVED);
		seedRegistrationIfMissing(tournament31, team2, userA, RegistrationStatus.APPROVED);
		seedRegistrationIfMissing(tournament31, team3, userA, RegistrationStatus.APPROVED);
		seedRegistrationIfMissing(tournament31, team4, userA, RegistrationStatus.APPROVED);

		seedMatchesIfMissing(tournament31, team1, team2, team3, team4);

		for (int i = 1; i <= 16; i++) {
			String name = String.format("KO16 Team %02d", i);
			Team t = getOrCreateTeam(name, i == 1 ? userA : null);
			seedPlayersIfMissing(t, buildDefaultSquad(t, "Đội " + i));
			seedRegistrationIfMissing(tournament34, t, userA, RegistrationStatus.APPROVED);
		}

		var eplTeams = List.of(
				getOrCreateTeam("Arsenal", null),
				getOrCreateTeam("Manchester City", null),
				getOrCreateTeam("Liverpool", null),
				getOrCreateTeam("Chelsea", null),
				getOrCreateTeam("Manchester United", null),
				getOrCreateTeam("Tottenham Hotspur", null),
				getOrCreateTeam("Newcastle United", null),
				getOrCreateTeam("Aston Villa", null),
				getOrCreateTeam("Brighton & Hove Albion", null),
				getOrCreateTeam("West Ham United", null),
				getOrCreateTeam("Everton", null),
				getOrCreateTeam("Leicester City", null),
				getOrCreateTeam("Wolverhampton Wanderers", null),
				getOrCreateTeam("Crystal Palace", null),
				getOrCreateTeam("Brentford", null),
				getOrCreateTeam("Fulham", null)
		);

		for (Team t : eplTeams) {
			if (t == null) continue;
			seedPlayersIfMissing(t, buildEplSquad(t));
			seedRegistrationIfMissing(tournamentEpl16, t, userA, RegistrationStatus.APPROVED);
		}

		var laligaTeams = List.of(
				getOrCreateTeam("Real Madrid", null),
				getOrCreateTeam("FC Barcelona", null),
				getOrCreateTeam("Atlético Madrid", null),
				getOrCreateTeam("Sevilla", null),
				getOrCreateTeam("Valencia", null),
				getOrCreateTeam("Villarreal", null),
				getOrCreateTeam("Real Sociedad", null),
				getOrCreateTeam("Athletic Club", null)
		);

		for (Team t : laligaTeams) {
			if (t == null) continue;
			seedPlayersIfMissing(t, buildLaLigaSquad(t));
			seedRegistrationIfMissing(tournamentLaLiga8, t, userA, RegistrationStatus.APPROVED);
		}

		for (Team t : teamRepository.findAll()) {
			seedPlayersIfMissing(t, buildDefaultSquad(t, t.getName() == null ? "Đội" : t.getName()));
		}
		ensurePlayersHaveDefaultAvatar();

		seedTransactionsIfMissing(userA);
	}

	private AppUser getOrCreateAdmin() {
		return userRepository.findByEmail("admin@example.com").orElseGet(() -> {
			var admin = new AppUser("Admin", "admin@example.com");
			admin.setRole(UserRole.ADMIN);
			admin.setGender(Gender.OTHER);
			admin.setPasswordHash(passwordEncoder.encode("admin123"));
			return userRepository.save(admin);
		});
	}

	private AppUser getOrCreateUserA() {
		return userRepository.findByEmail("a@example.com").orElseGet(() -> {
			var userA = new AppUser("Huỳnh Văn A", "a@example.com");
			userA.setGender(Gender.MALE);
			userA.setAddress("Thủ Đức, TPHCM");
			userA.setDateOfBirth(LocalDate.of(2004, 7, 28));
			userA.setPasswordHash(passwordEncoder.encode("user123"));
			return userRepository.save(userA);
		});
	}

	private Team getOrCreateTeam(String name, AppUser captain) {
		return teamRepository.findByNameIgnoreCase(name).orElseGet(() -> {
			var team = new Team(name);
			if (captain != null) {
				team.setCaptain(captain);
			}
			return teamRepository.save(team);
		});
	}

	private void seedPlayersIfMissing(Team team, List<Player> players) {
		if (team == null || team.getId() == null) return;
		if (playerRepository.countByTeamId(team.getId()) > 0) return;
		playerRepository.saveAll(players);
	}

	private Tournament getOrCreateTournament31() {
		return findTournamentByName("HUTECH mở rộng lần 31").orElseGet(() -> {
			var tournament = new Tournament("HUTECH mở rộng lần 31");
			tournament.setOrganizer("Đỗ Thành Nhân");
			tournament.setMode(TournamentMode.KNOCKOUT);
			tournament.setPitchType(PitchType.PITCH_7);
			tournament.setTeamLimit(4);
			tournament.setImageUrl("/assets/general-overview/tournament.jpg");
			tournament.setDescription("Giải đấu demo để test màn Thông tin chung / Danh sách đội / Lịch thi đấu.");
			tournament.setStatus(TournamentStatus.LIVE);
			tournament.setStartDate(LocalDate.now().minusDays(1));
			tournament.setEndDate(LocalDate.now().plusDays(7));
			return tournamentRepository.save(tournament);
		});
	}

	private Tournament getOrCreateTournament32() {
		return findTournamentByName("HUTECH mở rộng lần 32").orElseGet(() -> {
			var tournament = new Tournament("HUTECH mở rộng lần 32");
			tournament.setOrganizer("Kiệt Chan");
			tournament.setMode(TournamentMode.GROUP_STAGE);
			tournament.setPitchType(PitchType.PITCH_7);
			tournament.setTeamLimit(8);
			tournament.setImageUrl("/assets/general-overview/tournament.jpg");
			tournament.setDescription("Giải đấu demo để test trạng thái 'Chưa có đội đăng ký'.");
			tournament.setStatus(TournamentStatus.UPCOMING);
			tournament.setStartDate(LocalDate.now().plusDays(3));
			tournament.setEndDate(LocalDate.now().plusDays(30));
			return tournamentRepository.save(tournament);
		});
	}

	private Tournament getOrCreateTournament34Knockout16() {
		return findTournamentByName("HUTECH mở rộng lần 34").orElseGet(() -> {
			var tournament = new Tournament("HUTECH mở rộng lần 34");
			tournament.setOrganizer("Demo Knockout 16");
			tournament.setMode(TournamentMode.KNOCKOUT);
			tournament.setPitchType(PitchType.PITCH_7);
			tournament.setTeamLimit(16);
			tournament.setImageUrl("/assets/general-overview/tournament.jpg");
			tournament.setDescription("Giải đấu demo knockout 16 đội để test chức năng chia cặp ngẫu nhiên.");
			tournament.setStatus(TournamentStatus.UPCOMING);
			tournament.setStartDate(LocalDate.now().plusDays(5));
			tournament.setEndDate(LocalDate.now().plusDays(35));
			return tournamentRepository.save(tournament);
		});
	}

	private Tournament getOrCreateTournamentEplGroup16Pitch5() {
		return findTournamentByName("Ngoại hạng Anh 5v5 - 16 đội").orElseGet(() -> {
			var tournament = new Tournament("Ngoại hạng Anh 5v5 - 16 đội");
			tournament.setOrganizer("Premier League Demo");
			tournament.setMode(TournamentMode.GROUP_STAGE);
			tournament.setPitchType(PitchType.PITCH_5);
			tournament.setTeamLimit(16);
			tournament.setImageUrl("/assets/general-overview/tournament.jpg");
			tournament.setDescription("Giải đấu demo chia bảng 16 đội, sân 5. Seed đội và cầu thủ theo Premier League.");
			tournament.setStatus(TournamentStatus.UPCOMING);
			tournament.setStartDate(LocalDate.now().plusDays(2));
			tournament.setEndDate(LocalDate.now().plusDays(45));
			return tournamentRepository.save(tournament);
		});
	}

	private Tournament getOrCreateTournamentLaLigaKnockout8() {
		return findTournamentByName("LaLiga Knockout - 8 đội").orElseGet(() -> {
			var tournament = new Tournament("LaLiga Knockout - 8 đội");
			tournament.setOrganizer("LaLiga Demo");
			tournament.setMode(TournamentMode.KNOCKOUT);
			tournament.setPitchType(PitchType.PITCH_7);
			tournament.setTeamLimit(8);
			tournament.setImageUrl("/assets/general-overview/tournament.jpg");
			tournament.setDescription("Giải đấu demo knockout 8 đội (LaLiga). Seed đội và cầu thủ theo các CLB LaLiga.");
			tournament.setStatus(TournamentStatus.UPCOMING);
			tournament.setStartDate(LocalDate.now().plusDays(4));
			tournament.setEndDate(LocalDate.now().plusDays(25));
			return tournamentRepository.save(tournament);
		});
	}

	private Optional<Tournament> findTournamentByName(String name) {
		return tournamentRepository.findAll().stream()
			.filter(t -> t != null && t.getName() != null && t.getName().equalsIgnoreCase(name))
			.findFirst();
	}

	private void seedRegistrationIfMissing(Tournament tournament, Team team, AppUser user, RegistrationStatus status) {
		if (tournament == null || tournament.getId() == null) return;
		if (team == null || team.getId() == null) return;
		if (registrationRepository.findByTournamentIdAndTeamId(tournament.getId(), team.getId()).isPresent()) return;
		registrationRepository.save(buildRegistration(tournament, team, user, status));
	}

	private void seedMatchesIfMissing(Tournament tournament, Team team1, Team team2, Team team3, Team team4) {
		if (tournament == null || tournament.getId() == null) return;
		if (!matchRepository.findByTournamentIdOrderByScheduledAtAsc(tournament.getId()).isEmpty()) return;

		var semi1 = new Match(tournament, team1, team2);
		semi1.setRoundName("Bán kết");
		semi1.setScheduledAt(LocalDateTime.now().minusHours(5));
		semi1.setHomeScore(3);
		semi1.setAwayScore(1);
		semi1.setStatus(MatchStatus.FINISHED);

		var semi2 = new Match(tournament, team3, team4);
		semi2.setRoundName("Bán kết");
		semi2.setScheduledAt(LocalDateTime.now().minusHours(4));
		semi2.setHomeScore(1);
		semi2.setAwayScore(4);
		semi2.setStatus(MatchStatus.FINISHED);

		var finalMatch = new Match(tournament, team1, team4);
		finalMatch.setRoundName("Chung kết");
		finalMatch.setScheduledAt(LocalDateTime.now().minusHours(2));
		finalMatch.setHomeScore(1);
		finalMatch.setAwayScore(2);
		finalMatch.setStatus(MatchStatus.FINISHED);

		matchRepository.saveAll(List.of(semi1, semi2, finalMatch));
	}

	private void seedTransactionsIfMissing(AppUser user) {
		if (user == null || user.getId() == null) return;
		if (!transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty()) return;

		var tx1 = new Transaction("TXN-0001", "Nạp tiền ví", new BigDecimal("200000.00"), user);
		tx1.setStatus(TransactionStatus.SUCCESS);
		var tx2 = new Transaction("TXN-0002", "Phí đăng ký giải", new BigDecimal("100000.00"), user);
		tx2.setStatus(TransactionStatus.SUCCESS);
		transactionRepository.saveAll(List.of(tx1, tx2));
	}

	private Player buildPlayer(Team team, String fullName, Integer number, String role, String position) {
		var p = new Player(fullName, team);
		p.setJerseyNumber(number);
		p.setRole(role);
		p.setPosition(position);
		p.setAvatarUrl(DEFAULT_PLAYER_AVATAR_URL);
		return p;
	}

	private void ensurePlayersHaveDefaultAvatar() {
		var players = playerRepository.findAll();
		if (players == null || players.isEmpty()) return;
		for (Player p : players) {
			if (p == null) continue;
			p.setAvatarUrl(DEFAULT_PLAYER_AVATAR_URL);
		}
		playerRepository.saveAll(players);
	}

	private List<Player> buildDefaultSquad(Team team, String prefix) {
		return List.of(
			buildPlayer(team, prefix + " - Cầu thủ 1", 10, "Cầu thủ", "FW"),
			buildPlayer(team, prefix + " - Cầu thủ 2", 7, "Cầu thủ", "MF"),
			buildPlayer(team, prefix + " - Cầu thủ 3", 9, "Cầu thủ", "FW"),
			buildPlayer(team, prefix + " - Cầu thủ 4", 4, "Cầu thủ", "DF"),
			buildPlayer(team, prefix + " - Cầu thủ 5", 5, "Cầu thủ", "DF"),
			buildPlayer(team, prefix + " - Cầu thủ 6", 11, "Cầu thủ", "FW"),
			buildPlayer(team, prefix + " - Thủ môn", 1, "Thủ môn", "GK")
		);
	}

	private List<Player> buildEplSquad(Team team) {
		String club = team == null || team.getName() == null ? "" : team.getName().trim();
		return switch (club) {
			case "Arsenal" -> List.of(
					buildPlayer(team, "David Raya", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Ben White", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "William Saliba", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Declan Rice", 41, "Cầu thủ", "MF"),
					buildPlayer(team, "Martin Ødegaard", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Bukayo Saka", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Gabriel Martinelli", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Gabriel Magalhães", 6, "Cầu thủ", "DF"),
					buildPlayer(team, "Kai Havertz", 29, "Cầu thủ", "FW"),
					buildPlayer(team, "Leandro Trossard", 19, "Cầu thủ", "FW")
			);
			case "Manchester City" -> List.of(
					buildPlayer(team, "Ederson", 31, "Thủ môn", "GK"),
					buildPlayer(team, "Rúben Dias", 3, "Cầu thủ", "DF"),
					buildPlayer(team, "Kyle Walker", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Rodri", 16, "Cầu thủ", "MF"),
					buildPlayer(team, "Kevin De Bruyne", 17, "Cầu thủ", "MF"),
					buildPlayer(team, "Erling Haaland", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Phil Foden", 47, "Cầu thủ", "FW"),
					buildPlayer(team, "Bernardo Silva", 20, "Cầu thủ", "MF"),
					buildPlayer(team, "Jack Grealish", 10, "Cầu thủ", "FW"),
					buildPlayer(team, "John Stones", 5, "Cầu thủ", "DF")
			);
			case "Liverpool" -> List.of(
					buildPlayer(team, "Alisson Becker", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Virgil van Dijk", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "Trent Alexander-Arnold", 66, "Cầu thủ", "DF"),
					buildPlayer(team, "Alexis Mac Allister", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Dominik Szoboszlai", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Mohamed Salah", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Darwin Núñez", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Andrew Robertson", 26, "Cầu thủ", "DF"),
					buildPlayer(team, "Luis Díaz", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Ibrahima Konaté", 5, "Cầu thủ", "DF")
			);
			case "Chelsea" -> List.of(
					buildPlayer(team, "Robert Sánchez", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Reece James", 24, "Cầu thủ", "DF"),
					buildPlayer(team, "Thiago Silva", 6, "Cầu thủ", "DF"),
					buildPlayer(team, "Enzo Fernández", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Moisés Caicedo", 25, "Cầu thủ", "MF"),
					buildPlayer(team, "Cole Palmer", 20, "Cầu thủ", "FW"),
					buildPlayer(team, "Nicolas Jackson", 15, "Cầu thủ", "FW"),
					buildPlayer(team, "Ben Chilwell", 21, "Cầu thủ", "DF"),
					buildPlayer(team, "Christopher Nkunku", 18, "Cầu thủ", "FW"),
					buildPlayer(team, "Levi Colwill", 26, "Cầu thủ", "DF")
			);
			case "Manchester United" -> List.of(
					buildPlayer(team, "André Onana", 24, "Thủ môn", "GK"),
					buildPlayer(team, "Raphaël Varane", 19, "Cầu thủ", "DF"),
					buildPlayer(team, "Lisandro Martínez", 6, "Cầu thủ", "DF"),
					buildPlayer(team, "Bruno Fernandes", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Casemiro", 18, "Cầu thủ", "MF"),
					buildPlayer(team, "Marcus Rashford", 10, "Cầu thủ", "FW"),
					buildPlayer(team, "Rasmus Højlund", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Luke Shaw", 23, "Cầu thủ", "DF"),
					buildPlayer(team, "Alejandro Garnacho", 17, "Cầu thủ", "FW"),
					buildPlayer(team, "Diogo Dalot", 20, "Cầu thủ", "DF")
			);
			case "Tottenham Hotspur" -> List.of(
					buildPlayer(team, "Guglielmo Vicario", 13, "Thủ môn", "GK"),
					buildPlayer(team, "Cristian Romero", 17, "Cầu thủ", "DF"),
					buildPlayer(team, "Micky van de Ven", 37, "Cầu thủ", "DF"),
					buildPlayer(team, "James Maddison", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Yves Bissouma", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Heung-min Son", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Richarlison", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Pedro Porro", 23, "Cầu thủ", "DF"),
					buildPlayer(team, "Dejan Kulusevski", 21, "Cầu thủ", "FW"),
					buildPlayer(team, "Rodrigo Bentancur", 30, "Cầu thủ", "MF")
			);
			case "Newcastle United" -> List.of(
					buildPlayer(team, "Nick Pope", 22, "Thủ môn", "GK"),
					buildPlayer(team, "Kieran Trippier", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Sven Botman", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "Bruno Guimarães", 39, "Cầu thủ", "MF"),
					buildPlayer(team, "Joelinton", 7, "Cầu thủ", "MF"),
					buildPlayer(team, "Alexander Isak", 14, "Cầu thủ", "FW"),
					buildPlayer(team, "Anthony Gordon", 10, "Cầu thủ", "FW"),
					buildPlayer(team, "Dan Burn", 33, "Cầu thủ", "DF"),
					buildPlayer(team, "Miguel Almirón", 24, "Cầu thủ", "FW"),
					buildPlayer(team, "Sean Longstaff", 36, "Cầu thủ", "MF")
			);
			case "Aston Villa" -> List.of(
					buildPlayer(team, "Emiliano Martínez", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Matty Cash", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Pau Torres", 14, "Cầu thủ", "DF"),
					buildPlayer(team, "Douglas Luiz", 6, "Cầu thủ", "MF"),
					buildPlayer(team, "John McGinn", 7, "Cầu thủ", "MF"),
					buildPlayer(team, "Ollie Watkins", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Leon Bailey", 31, "Cầu thủ", "FW"),
					buildPlayer(team, "Ezri Konsa", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "Moussa Diaby", 19, "Cầu thủ", "FW"),
					buildPlayer(team, "Youri Tielemans", 8, "Cầu thủ", "MF")
			);
			case "Brighton & Hove Albion" -> List.of(
					buildPlayer(team, "Jason Steele", 23, "Thủ môn", "GK"),
					buildPlayer(team, "Lewis Dunk", 5, "Cầu thủ", "DF"),
					buildPlayer(team, "Pervis Estupiñán", 30, "Cầu thủ", "DF"),
					buildPlayer(team, "Pascal Groß", 13, "Cầu thủ", "MF"),
					buildPlayer(team, "Billy Gilmour", 11, "Cầu thủ", "MF"),
					buildPlayer(team, "Kaoru Mitoma", 22, "Cầu thủ", "FW"),
					buildPlayer(team, "João Pedro", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Solly March", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Evan Ferguson", 28, "Cầu thủ", "FW"),
					buildPlayer(team, "Tariq Lamptey", 2, "Cầu thủ", "DF")
			);
			case "West Ham United" -> List.of(
					buildPlayer(team, "Alphonse Areola", 23, "Thủ môn", "GK"),
					buildPlayer(team, "Kurt Zouma", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "Aaron Wan-Bissaka", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "James Ward-Prowse", 7, "Cầu thủ", "MF"),
					buildPlayer(team, "Lucas Paquetá", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Jarrod Bowen", 20, "Cầu thủ", "FW"),
					buildPlayer(team, "Michail Antonio", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Mohammed Kudus", 14, "Cầu thủ", "FW"),
					buildPlayer(team, "Tomáš Souček", 28, "Cầu thủ", "MF"),
					buildPlayer(team, "Nayef Aguerd", 27, "Cầu thủ", "DF")
			);
			case "Everton" -> List.of(
					buildPlayer(team, "Jordan Pickford", 1, "Thủ môn", "GK"),
					buildPlayer(team, "James Tarkowski", 6, "Cầu thủ", "DF"),
					buildPlayer(team, "Jarrad Branthwaite", 32, "Cầu thủ", "DF"),
					buildPlayer(team, "Amadou Onana", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Abdoulaye Doucouré", 16, "Cầu thủ", "MF"),
					buildPlayer(team, "Dominic Calvert-Lewin", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Dwight McNeil", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Ashley Young", 18, "Cầu thủ", "DF"),
					buildPlayer(team, "Jack Harrison", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Idrissa Gueye", 27, "Cầu thủ", "MF")
			);
			case "Leicester City" -> List.of(
					buildPlayer(team, "Mads Hermansen", 30, "Thủ môn", "GK"),
					buildPlayer(team, "Wout Faes", 3, "Cầu thủ", "DF"),
					buildPlayer(team, "James Justin", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Wilfred Ndidi", 25, "Cầu thủ", "MF"),
					buildPlayer(team, "Kiernan Dewsbury-Hall", 22, "Cầu thủ", "MF"),
					buildPlayer(team, "Jamie Vardy", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Kelechi Iheanacho", 14, "Cầu thủ", "FW"),
					buildPlayer(team, "Ricardo Pereira", 21, "Cầu thủ", "DF"),
					buildPlayer(team, "Harvey Barnes", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Youri Tielemans", 8, "Cầu thủ", "MF")
			);
			case "Wolverhampton Wanderers" -> List.of(
					buildPlayer(team, "José Sá", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Craig Dawson", 15, "Cầu thủ", "DF"),
					buildPlayer(team, "Max Kilman", 23, "Cầu thủ", "DF"),
					buildPlayer(team, "Mario Lemina", 5, "Cầu thủ", "MF"),
					buildPlayer(team, "João Gomes", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Matheus Cunha", 12, "Cầu thủ", "FW"),
					buildPlayer(team, "Hwang Hee-chan", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Pedro Neto", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Rayan Aït-Nouri", 3, "Cầu thủ", "DF"),
					buildPlayer(team, "Pablo Sarabia", 21, "Cầu thủ", "FW")
			);
			case "Crystal Palace" -> List.of(
					buildPlayer(team, "Sam Johnstone", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Marc Guéhi", 6, "Cầu thủ", "DF"),
					buildPlayer(team, "Joachim Andersen", 16, "Cầu thủ", "DF"),
					buildPlayer(team, "Eberechi Eze", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Cheick Doucouré", 28, "Cầu thủ", "MF"),
					buildPlayer(team, "Michael Olise", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Jean-Philippe Mateta", 14, "Cầu thủ", "FW"),
					buildPlayer(team, "Tyrick Mitchell", 3, "Cầu thủ", "DF"),
					buildPlayer(team, "Jordan Ayew", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Jefferson Lerma", 8, "Cầu thủ", "MF")
			);
			case "Brentford" -> List.of(
					buildPlayer(team, "Mark Flekken", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Ethan Pinnock", 5, "Cầu thủ", "DF"),
					buildPlayer(team, "Ben Mee", 16, "Cầu thủ", "DF"),
					buildPlayer(team, "Christian Nørgaard", 6, "Cầu thủ", "MF"),
					buildPlayer(team, "Mathias Jensen", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Ivan Toney", 17, "Cầu thủ", "FW"),
					buildPlayer(team, "Bryan Mbeumo", 19, "Cầu thủ", "FW"),
					buildPlayer(team, "Yoane Wissa", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Aaron Hickey", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Vitaly Janelt", 27, "Cầu thủ", "MF")
			);
			case "Fulham" -> List.of(
					buildPlayer(team, "Bernd Leno", 17, "Thủ môn", "GK"),
					buildPlayer(team, "Tim Ream", 13, "Cầu thủ", "DF"),
					buildPlayer(team, "Tosin Adarabioyo", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "João Palhinha", 26, "Cầu thủ", "MF"),
					buildPlayer(team, "Andreas Pereira", 18, "Cầu thủ", "MF"),
					buildPlayer(team, "Aleksandar Mitrović", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Willian", 20, "Cầu thủ", "FW"),
					buildPlayer(team, "Bobby De Cordova-Reid", 14, "Cầu thủ", "FW"),
					buildPlayer(team, "Antonee Robinson", 33, "Cầu thủ", "DF"),
					buildPlayer(team, "Harrison Reed", 6, "Cầu thủ", "MF")
			);
			default -> List.of(
					buildPlayer(team, club + " - Thủ môn", 1, "Thủ môn", "GK"),
					buildPlayer(team, club + " - Hậu vệ 1", 2, "Cầu thủ", "DF"),
					buildPlayer(team, club + " - Hậu vệ 2", 5, "Cầu thủ", "DF"),
					buildPlayer(team, club + " - Tiền vệ 1", 8, "Cầu thủ", "MF"),
					buildPlayer(team, club + " - Tiền vệ 2", 10, "Cầu thủ", "MF"),
					buildPlayer(team, club + " - Tiền đạo 1", 9, "Cầu thủ", "FW"),
					buildPlayer(team, club + " - Tiền đạo 2", 11, "Cầu thủ", "FW"),
					buildPlayer(team, club + " - Dự bị 1", 14, "Cầu thủ", "MF"),
					buildPlayer(team, club + " - Dự bị 2", 16, "Cầu thủ", "DF"),
					buildPlayer(team, club + " - Dự bị 3", 20, "Cầu thủ", "FW")
			);
		};
	}

	private List<Player> buildLaLigaSquad(Team team) {
		String club = team == null || team.getName() == null ? "" : team.getName().trim();
		return switch (club) {
			case "Real Madrid" -> List.of(
					buildPlayer(team, "Thibaut Courtois", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Dani Carvajal", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Antonio Rüdiger", 22, "Cầu thủ", "DF"),
					buildPlayer(team, "Jude Bellingham", 5, "Cầu thủ", "MF"),
					buildPlayer(team, "Toni Kroos", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Federico Valverde", 15, "Cầu thủ", "MF"),
					buildPlayer(team, "Vinícius Júnior", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Rodrygo", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Kylian Mbappé", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Eduardo Camavinga", 12, "Cầu thủ", "MF")
			);
			case "FC Barcelona" -> List.of(
					buildPlayer(team, "Marc-André ter Stegen", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Ronald Araújo", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "Jules Koundé", 23, "Cầu thủ", "DF"),
					buildPlayer(team, "Pedri", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Frenkie de Jong", 21, "Cầu thủ", "MF"),
					buildPlayer(team, "Gavi", 6, "Cầu thủ", "MF"),
					buildPlayer(team, "Robert Lewandowski", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Raphinha", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Lamine Yamal", 27, "Cầu thủ", "FW"),
					buildPlayer(team, "Alejandro Balde", 3, "Cầu thủ", "DF")
			);
			case "Atlético Madrid" -> List.of(
					buildPlayer(team, "Jan Oblak", 13, "Thủ môn", "GK"),
					buildPlayer(team, "José María Giménez", 2, "Cầu thủ", "DF"),
					buildPlayer(team, "Stefan Savić", 15, "Cầu thủ", "DF"),
					buildPlayer(team, "Koke", 6, "Cầu thủ", "MF"),
					buildPlayer(team, "Rodrigo De Paul", 5, "Cầu thủ", "MF"),
					buildPlayer(team, "Marcos Llorente", 14, "Cầu thủ", "MF"),
					buildPlayer(team, "Antoine Griezmann", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Álvaro Morata", 19, "Cầu thủ", "FW"),
					buildPlayer(team, "Samuel Lino", 12, "Cầu thủ", "FW"),
					buildPlayer(team, "Nahuel Molina", 16, "Cầu thủ", "DF")
			);
			case "Sevilla" -> List.of(
					buildPlayer(team, "Yassine Bounou", 13, "Thủ môn", "GK"),
					buildPlayer(team, "Sergio Ramos", 4, "Cầu thủ", "DF"),
					buildPlayer(team, "Jesús Navas", 16, "Cầu thủ", "DF"),
					buildPlayer(team, "Ivan Rakitić", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Fernando", 20, "Cầu thủ", "MF"),
					buildPlayer(team, "Óliver Torres", 21, "Cầu thủ", "MF"),
					buildPlayer(team, "Youssef En-Nesyri", 15, "Cầu thủ", "FW"),
					buildPlayer(team, "Lucas Ocampos", 5, "Cầu thủ", "FW"),
					buildPlayer(team, "Suso", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Marcos Acuña", 19, "Cầu thủ", "DF")
			);
			case "Valencia" -> List.of(
					buildPlayer(team, "Giorgi Mamardashvili", 25, "Thủ môn", "GK"),
					buildPlayer(team, "José Gayà", 14, "Cầu thủ", "DF"),
					buildPlayer(team, "Gabriel Paulista", 5, "Cầu thủ", "DF"),
					buildPlayer(team, "Javi Guerra", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "André Almeida", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Pepelu", 18, "Cầu thủ", "MF"),
					buildPlayer(team, "Hugo Duro", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Diego López", 16, "Cầu thủ", "FW"),
					buildPlayer(team, "Sergi Canós", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Dimitri Foulquier", 12, "Cầu thủ", "DF")
			);
			case "Villarreal" -> List.of(
					buildPlayer(team, "Gerónimo Rulli", 13, "Thủ môn", "GK"),
					buildPlayer(team, "Raúl Albiol", 3, "Cầu thủ", "DF"),
					buildPlayer(team, "Juan Foyth", 8, "Cầu thủ", "DF"),
					buildPlayer(team, "Dani Parejo", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Álex Baena", 16, "Cầu thủ", "MF"),
					buildPlayer(team, "Étienne Capoue", 6, "Cầu thủ", "MF"),
					buildPlayer(team, "Gerard Moreno", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Alexander Sørloth", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Yeremy Pino", 21, "Cầu thủ", "FW"),
					buildPlayer(team, "Alfonso Pedraza", 24, "Cầu thủ", "DF")
			);
			case "Real Sociedad" -> List.of(
					buildPlayer(team, "Álex Remiro", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Robin Le Normand", 24, "Cầu thủ", "DF"),
					buildPlayer(team, "Aritz Elustondo", 6, "Cầu thủ", "DF"),
					buildPlayer(team, "Mikel Merino", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Martín Zubimendi", 4, "Cầu thủ", "MF"),
					buildPlayer(team, "Brais Méndez", 23, "Cầu thủ", "MF"),
					buildPlayer(team, "Takefusa Kubo", 14, "Cầu thủ", "FW"),
					buildPlayer(team, "Mikel Oyarzabal", 10, "Cầu thủ", "FW"),
					buildPlayer(team, "Ander Barrenetxea", 7, "Cầu thủ", "FW"),
					buildPlayer(team, "Kieran Tierney", 3, "Cầu thủ", "DF")
			);
			case "Athletic Club" -> List.of(
					buildPlayer(team, "Unai Simón", 1, "Thủ môn", "GK"),
					buildPlayer(team, "Dani Vivian", 3, "Cầu thủ", "DF"),
					buildPlayer(team, "Iñigo Lekue", 15, "Cầu thủ", "DF"),
					buildPlayer(team, "Oihan Sancet", 8, "Cầu thủ", "MF"),
					buildPlayer(team, "Iker Muniain", 10, "Cầu thủ", "MF"),
					buildPlayer(team, "Dani García", 14, "Cầu thủ", "MF"),
					buildPlayer(team, "Iñaki Williams", 9, "Cầu thủ", "FW"),
					buildPlayer(team, "Nico Williams", 11, "Cầu thủ", "FW"),
					buildPlayer(team, "Gorka Guruzeta", 12, "Cầu thủ", "FW"),
					buildPlayer(team, "Yuri Berchiche", 17, "Cầu thủ", "DF")
			);
			default -> buildDefaultSquad(team, club.isBlank() ? "LaLiga Team" : club);
		};
	}

	private TournamentRegistration buildRegistration(
		Tournament tournament,
		Team team,
		AppUser user,
		RegistrationStatus status
	) {
		var r = new TournamentRegistration(tournament, team);
		r.setRegisteredBy(user);
		r.setStatus(status);
		return r;
	}
}

