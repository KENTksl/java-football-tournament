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

@Component
public class DataSeeder implements CommandLineRunner {
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
		if (tournamentRepository.count() > 0) {
			return;
		}

		var admin = new AppUser("Admin", "admin@example.com");
		admin.setRole(UserRole.ADMIN);
		admin.setGender(Gender.OTHER);
		admin.setPasswordHash(passwordEncoder.encode("admin123"));
		userRepository.save(admin);

		var userA = new AppUser("Huỳnh Văn A", "a@example.com");
		userA.setGender(Gender.MALE);
		userA.setAddress("Thủ Đức, TPHCM");
		userA.setDateOfBirth(LocalDate.of(2004, 7, 28));
		userA.setPasswordHash(passwordEncoder.encode("user123"));
		userRepository.save(userA);

		var team1 = new Team("FC Test 19");
		team1.setCaptain(userA);
		teamRepository.save(team1);

		var team2 = new Team("FC Test 18");
		teamRepository.save(team2);

		var team3 = new Team("FC Test 17");
		teamRepository.save(team3);

		var team4 = new Team("22DTHC3");
		teamRepository.save(team4);

		playerRepository.saveAll(List.of(
			buildPlayer(team1, "Nguyễn Văn B", 10, "Cầu thủ", "FW"),
			buildPlayer(team1, "Trần Văn C", 7, "Cầu thủ", "MF"),
			buildPlayer(team1, "Lê Văn D", 1, "Thủ môn", "GK"),
			buildPlayer(team4, "Phạm Văn E", 9, "Cầu thủ", "FW"),
			buildPlayer(team4, "Võ Văn F", 4, "Cầu thủ", "DF")
		));

		var tournament = new Tournament("HUTECH mở rộng lần 31");
		tournament.setOrganizer("Đỗ Thành Nhân");
		tournament.setMode(TournamentMode.KNOCKOUT);
		tournament.setPitchType(PitchType.PITCH_7);
		tournament.setTeamLimit(4);
		tournament.setImageUrl("/assets/general-overview/tournament.jpg");
		tournament.setDescription("Giải đấu demo từ dữ liệu mẫu UI.");
		tournament.setStatus(TournamentStatus.LIVE);
		tournament.setStartDate(LocalDate.now().minusDays(1));
		tournament.setEndDate(LocalDate.now().plusDays(7));
		tournamentRepository.save(tournament);

		registrationRepository.saveAll(List.of(
			buildRegistration(tournament, team1, userA, RegistrationStatus.APPROVED),
			buildRegistration(tournament, team2, userA, RegistrationStatus.APPROVED),
			buildRegistration(tournament, team3, userA, RegistrationStatus.APPROVED),
			buildRegistration(tournament, team4, userA, RegistrationStatus.APPROVED)
		));

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

		var tx1 = new Transaction("TXN-0001", "Nạp tiền ví", new BigDecimal("200000.00"), userA);
		tx1.setStatus(TransactionStatus.SUCCESS);
		var tx2 = new Transaction("TXN-0002", "Phí đăng ký giải", new BigDecimal("100000.00"), userA);
		tx2.setStatus(TransactionStatus.SUCCESS);
		transactionRepository.saveAll(List.of(tx1, tx2));
	}

	private Player buildPlayer(Team team, String fullName, Integer number, String role, String position) {
		var p = new Player(fullName, team);
		p.setJerseyNumber(number);
		p.setRole(role);
		p.setPosition(position);
		return p;
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

