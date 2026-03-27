package com.example.football_tourament_web.service.user;

import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.MatchEvent;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.PitchType;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TeamSide;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.repository.MatchEventRepository;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.common.FileStorageService;
import com.example.football_tourament_web.service.common.ViewFormatService;
import com.example.football_tourament_web.service.core.MatchLineupService;
import com.example.football_tourament_web.service.core.MatchService;
import com.example.football_tourament_web.service.core.TeamService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TournamentService;
import com.example.football_tourament_web.service.core.TransactionService;
import com.example.football_tourament_web.service.core.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserTournamentViewService {
	private final TournamentService tournamentService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final MatchService matchService;
	private final MatchLineupService matchLineupService;
	private final TeamService teamService;
	private final UserService userService;
	private final TransactionService transactionService;
	private final ViewFormatService viewFormatService;
	private final PlayerRepository playerRepository;
	private final FileStorageService fileStorageService;

	private final MatchEventRepository matchEventRepository;

	public UserTournamentViewService(
			TournamentService tournamentService,
			TournamentRegistrationService tournamentRegistrationService,
			MatchService matchService,
			MatchLineupService matchLineupService,
			TeamService teamService,
			UserService userService,
			TransactionService transactionService,
			ViewFormatService viewFormatService,
			PlayerRepository playerRepository,
			MatchEventRepository matchEventRepository,
			FileStorageService fileStorageService
	) {
		this.tournamentService = tournamentService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.matchService = matchService;
		this.matchLineupService = matchLineupService;
		this.teamService = teamService;
		this.userService = userService;
		this.transactionService = transactionService;
		this.viewFormatService = viewFormatService;
		this.playerRepository = playerRepository;
		this.matchEventRepository = matchEventRepository;
		this.fileStorageService = fileStorageService;
	}

	@Transactional(readOnly = true)
	public Tournament findTournamentOrNull(Long id) {
		if (id == null) {
			return null;
		}
		return tournamentService.findById(id).orElse(null);
	}

	@Transactional(readOnly = true)
	public PaymentInfo buildPaymentInfo(Authentication authentication, Long tournamentId) {
		if (tournamentId == null) {
			return new PaymentInfo(BigDecimal.ZERO, BigDecimal.ZERO, viewFormatService.formatMoney(BigDecimal.ZERO), viewFormatService.formatMoney(BigDecimal.ZERO), true);
		}
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		BigDecimal fee = tournament == null || tournament.getRegistrationFee() == null ? BigDecimal.ZERO : tournament.getRegistrationFee();
		if (fee.signum() < 0) fee = BigDecimal.ZERO;

		var user = requireCurrentUser(authentication);
		BigDecimal balance = user == null ? BigDecimal.ZERO : transactionService.calculateBalance(user.getId());
		if (balance == null) balance = BigDecimal.ZERO;
		boolean enough = balance.compareTo(fee) >= 0;
		return new PaymentInfo(fee, balance, viewFormatService.formatMoney(fee), viewFormatService.formatMoney(balance), enough);
	}

	@Transactional(readOnly = true)
	public long countRegisteredTeams(Long tournamentId) {
		return tournamentRegistrationService.countRegisteredTeams(tournamentId);
	}

	@Transactional(readOnly = true)
	public Map<Long, Long> buildRegisteredTeamCountMap(List<Tournament> tournaments) {
		Map<Long, Long> result = new HashMap<>();
		if (tournaments == null) {
			return result;
		}
		for (Tournament tournament : tournaments) {
			if (tournament == null || tournament.getId() == null) {
				continue;
			}
			result.put(tournament.getId(), tournamentRegistrationService.countRegisteredTeams(tournament.getId()));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public Map<Long, Integer> buildRegisteredTeamPercentMap(List<Tournament> tournaments) {
		Map<Long, Integer> result = new HashMap<>();
		if (tournaments == null) {
			return result;
		}
		for (Tournament tournament : tournaments) {
			if (tournament == null || tournament.getId() == null) {
				continue;
			}
			long count = tournamentRegistrationService.countRegisteredTeams(tournament.getId());
			int percent = 0;
			if (tournament.getTeamLimit() != null && tournament.getTeamLimit() > 0) {
				percent = (int) Math.min(100, (count * 100 / tournament.getTeamLimit()));
			}
			result.put(tournament.getId(), percent);
		}
		return result;
	}

	public boolean isTournamentFull(Tournament tournament) {
		if (tournament == null || tournament.getId() == null || tournament.getTeamLimit() == null) {
			return false;
		}
		return tournamentRegistrationService.countRegisteredTeams(tournament.getId()) >= tournament.getTeamLimit();
	}

	@Transactional(readOnly = true)
	public TeamPrefillResponse buildTeamPrefill(Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return TeamPrefillResponse.notFound("Vui lòng đăng nhập để dùng dữ liệu đội");
		}

		var teamOpt = teamService.listByCaptain(user.getId()).stream().findFirst();
		if (teamOpt.isEmpty()) {
			return TeamPrefillResponse.notFound("Bạn chưa có đội bóng để sử dụng");
		}

		var team = teamOpt.get();
		var players = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId()).stream()
				.map(player -> new PlayerPrefill(player.getFullName(), player.getJerseyNumber(), player.getAvatarUrl()))
				.toList();

		return TeamPrefillResponse.found(
				team.getId(),
				team.getName(),
				user.getFullName(),
				user.getEmail(),
				user.getPhone(),
				team.getLogoUrl(),
				players
		);
	}

	@Transactional(readOnly = true)
	public TeamPrefillResponse buildTeamPrefillForTeam(Authentication authentication, Long teamId) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return TeamPrefillResponse.notFound("Vui lòng đăng nhập để dùng dữ liệu đội");
		}
		if (teamId == null) {
			return TeamPrefillResponse.notFound("Thiếu teamId");
		}

		Team team = teamService.findByIdWithCaptain(teamId).orElse(null);
		if (team == null || team.getCaptain() == null || team.getCaptain().getId() == null || !team.getCaptain().getId().equals(user.getId())) {
			return TeamPrefillResponse.notFound("Bạn không có quyền xem đội này");
		}

		var players = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId()).stream()
				.map(player -> new PlayerPrefill(player.getFullName(), player.getJerseyNumber(), player.getAvatarUrl()))
				.toList();

		return TeamPrefillResponse.found(
				team.getId(),
				team.getName(),
				team.getCaptain() != null ? team.getCaptain().getFullName() : user.getFullName(),
				team.getCaptain() != null ? team.getCaptain().getEmail() : user.getEmail(),
				team.getCaptain() != null ? team.getCaptain().getPhone() : user.getPhone(),
				team.getLogoUrl(),
				players
		);
	}

	@Transactional(readOnly = true)
	public List<SignUpTeamOption> listSignUpTeamOptions(Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return List.of();
		}
		return teamService.listByCaptain(user.getId()).stream()
				.filter(team -> team != null && team.getId() != null)
				.map(team -> new SignUpTeamOption(team.getId(), team.getName()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SignUpTeamPrefill> listSignUpTeamPrefills(Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return List.of();
		}
		List<SignUpTeamPrefill> result = new ArrayList<>();
		for (var team : teamService.listByCaptain(user.getId())) {
			if (team == null || team.getId() == null) {
				continue;
			}
			String representativeName = team.getCaptain() != null ? team.getCaptain().getFullName() : user.getFullName();
			String contactEmail = team.getCaptain() != null ? team.getCaptain().getEmail() : user.getEmail();
			String contactPhone = team.getCaptain() != null ? team.getCaptain().getPhone() : user.getPhone();
			result.add(new SignUpTeamPrefill(
					team.getId(),
					team.getName(),
					representativeName,
					contactEmail,
					contactPhone,
					team.getLogoUrl(),
					List.of()
			));
		}
		return result;
	}

	@Transactional
	public RegistrationSubmitResult submitRegistration(
			Authentication authentication,
			Long tournamentId,
			Long teamId,
			String teamName,
			MultipartFile logoFile
	) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return RegistrationSubmitResult.failed("Vui lòng đăng nhập để đăng ký");
		}
		if (tournamentId == null) {
			return RegistrationSubmitResult.failed("Thiếu thông tin giải đấu");
		}

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return RegistrationSubmitResult.failed("Không tìm thấy giải đấu");
		}

		Team team;
		if (teamId != null) {
			var teamOpt = teamService.findById(teamId);
			if (teamOpt.isEmpty()) {
				return RegistrationSubmitResult.failed("Không tìm thấy đội bóng");
			}
			team = teamOpt.get();
			if (team.getCaptain() == null || team.getCaptain().getId() == null || !team.getCaptain().getId().equals(user.getId())) {
				return RegistrationSubmitResult.failed("Bạn không có quyền đăng ký đội bóng này");
			}
		} else {
			String name = teamName == null ? "" : teamName.trim();
			if (name.isBlank()) {
				return RegistrationSubmitResult.failed("Vui lòng nhập tên đội bóng");
			}

			if (teamService.findByName(name).isPresent()) {
				return RegistrationSubmitResult.failed("Đã có đội với tên này, vui lòng đặt tên khác");
			}

			if (teamService.countByCaptain(user.getId()) >= 2) {
				return RegistrationSubmitResult.failed("Bạn đã đạt giới hạn số đội có thể tạo");
			}

			String logoUrl = null;
			try {
				if (logoFile != null && !logoFile.isEmpty()) {
					logoUrl = fileStorageService.storeValidatedImageUnderUploads(logoFile, "teams", 2L * 1024 * 1024);
				}
			} catch (FileStorageService.FileTooLargeException ex) {
				return RegistrationSubmitResult.failed("Logo quá lớn (tối đa 2MB)");
			} catch (FileStorageService.InvalidFileTypeException ex) {
				return RegistrationSubmitResult.failed("Logo không đúng định dạng (jpg/png/webp)");
			} catch (Exception ex) {
				return RegistrationSubmitResult.failed("Không thể tải logo lên");
			}

			Team newTeam = new Team(name);
			newTeam.setCaptain(user);
			newTeam.setLogoUrl(logoUrl);
			team = teamService.save(newTeam);
			teamId = team.getId();
		}

		var existing = tournamentRegistrationService.findByTournamentAndTeam(tournamentId, teamId).orElse(null);
		if (existing != null && existing.getStatus() == RegistrationStatus.APPROVED) {
			return RegistrationSubmitResult.failed("Đội đã được duyệt trong giải đấu này");
		}
		if (existing != null && existing.getStatus() == RegistrationStatus.PENDING) {
			return RegistrationSubmitResult.failed("Đội đã gửi hồ sơ và đang chờ duyệt");
		}
		long registeredTeams = tournamentRegistrationService.countRegisteredTeams(tournamentId);
		if (tournament.getTeamLimit() != null && registeredTeams >= tournament.getTeamLimit()) {
			return RegistrationSubmitResult.failed("Giải đấu đã đủ số lượng đội tham gia");
		}
		if (existing == null) {
			existing = new TournamentRegistration();
			existing.setTournament(tournament);
			existing.setTeam(team);
		} else if (existing.getStatus() == RegistrationStatus.REJECTED) {
			if (existing.getPaidAmount() != null && existing.getPaidAmount().signum() > 0 && existing.getRefundTransactionCode() == null) {
				return RegistrationSubmitResult.failed("Hồ sơ trước đó đã bị từ chối nhưng chưa hoàn phí. Vui lòng liên hệ admin để được hỗ trợ.");
			}
		}

		BigDecimal fee = tournament.getRegistrationFee() == null ? BigDecimal.ZERO : tournament.getRegistrationFee();
		if (fee.signum() < 0) {
			fee = BigDecimal.ZERO;
		}
		if (fee.signum() > 0) {
			BigDecimal balance = transactionService.calculateBalance(user.getId());
			if (balance == null) balance = BigDecimal.ZERO;
			if (balance.compareTo(fee) < 0) {
				return RegistrationSubmitResult.failed("Số dư không đủ để đăng ký. Phí đăng ký: " + viewFormatService.formatMoney(fee) + ".");
			}

			String txCode = "REG_FEE_" + tournamentId + "_" + UUID.randomUUID();
			Transaction tx = new Transaction(
					txCode,
					"Phí đăng ký giải đấu: " + (tournament.getName() == null ? ("#" + tournament.getId()) : tournament.getName()),
					fee.negate(),
					user
			);
			tx.setStatus(TransactionStatus.SUCCESS);
			transactionService.save(tx);

			BigDecimal pool = tournament.getPrizePool() == null ? BigDecimal.ZERO : tournament.getPrizePool();
			tournament.setPrizePool(pool.add(fee));
			tournamentService.save(tournament);

			existing.setPaidAmount(fee);
			existing.setPaidTransactionCode(txCode);
			existing.setRefundedAmount(BigDecimal.ZERO);
			existing.setRefundTransactionCode(null);
			existing.setRefundedAt(null);
		}
		existing.setRegisteredBy(user);
		existing.setStatus(RegistrationStatus.PENDING);
		existing.setGroupName(null);
		tournamentRegistrationService.save(existing);
		if (fee.signum() > 0) {
			return RegistrationSubmitResult.success("Đã gửi hồ sơ và trừ phí đăng ký (" + viewFormatService.formatMoney(fee) + "). Admin sẽ duyệt trong thời gian sớm nhất");
		}
		return RegistrationSubmitResult.success("Đã gửi hồ sơ đăng ký. Admin sẽ duyệt trong thời gian sớm nhất");
	}

	@Transactional(readOnly = true)
	public List<TeamCardView> buildTournamentTeams(Long tournamentId) {
		if (tournamentId == null) {
			return List.of();
		}
		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<TeamCardView> teams = new ArrayList<>();
		Set<Long> seenTeamIds = new HashSet<>();
		for (TournamentRegistration registration : registrations) {
			if (registration == null || registration.getStatus() != RegistrationStatus.APPROVED) {
				continue;
			}
			if (registration.getTeam() == null || registration.getTeam().getId() == null) {
				continue;
			}
			Long teamId = registration.getTeam().getId();
			if (seenTeamIds.contains(teamId)) {
				continue;
			}
			seenTeamIds.add(teamId);
			String teamName = registration.getTeam().getName();
			String captainName = registration.getTeam().getCaptain() == null ? null : registration.getTeam().getCaptain().getFullName();
			long memberCount = playerRepository.countByTeamId(teamId);
			String teamCode = "TU" + teamId;
			String coverLetter = (teamName == null || teamName.isBlank()) ? "T" : String.valueOf(Character.toUpperCase(teamName.trim().charAt(0)));
			teams.add(new TeamCardView(
					teamId,
					teamName,
					captainName,
					teamCode,
					memberCount,
					coverLetter,
					registration.getTeam().getLogoUrl()
			));
		}
		teams.sort(Comparator.comparing(TeamCardView::teamName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
		return teams;
	}

	@Transactional(readOnly = true)
	public MatchLineupView buildMatchLineupView(Long tournamentId, Long matchId) {
		if (tournamentId == null || matchId == null) {
			return MatchLineupView.notFound("Thiếu thông tin trận đấu");
		}
		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return MatchLineupView.notFound("Không tìm thấy dữ liệu đội hình");
		}
		if (match.getHomeTeam() == null || match.getAwayTeam() == null
				|| match.getHomeTeam().getId() == null || match.getAwayTeam().getId() == null) {
			return MatchLineupView.notFound("Trận đấu chưa có đủ thông tin 2 đội");
		}

		List<LineupSlotView> pitchSlots = buildPitchSlots(match.getTournament().getPitchType());
		Map<Integer, LineupSlotPlayerView> homeAssigned = new HashMap<>();
		Map<Integer, LineupSlotPlayerView> awayAssigned = new HashMap<>();
		Set<Long> homeAssignedPlayerIds = new HashSet<>();
		Set<Long> awayAssignedPlayerIds = new HashSet<>();

		for (var slot : matchLineupService.listByMatchId(matchId)) {
			if (slot == null || slot.getPlayer() == null || slot.getPlayer().getId() == null || slot.getSlotIndex() == null) {
				continue;
			}
			PlayerLineupView playerView = new PlayerLineupView(
					slot.getPlayer().getId(),
					slot.getPlayer().getFullName(),
					slot.getPlayer().getJerseyNumber(),
					slot.getPlayer().getPosition(),
					slot.getPlayer().getAvatarUrl()
			);
			LineupSlotPlayerView slotPlayerView = new LineupSlotPlayerView(slot.getSlotIndex(), slot.getPosition(), playerView);
			if (slot.getTeamSide() == TeamSide.HOME) {
				homeAssigned.put(slot.getSlotIndex(), slotPlayerView);
				homeAssignedPlayerIds.add(slot.getPlayer().getId());
			}
			if (slot.getTeamSide() == TeamSide.AWAY) {
				awayAssigned.put(slot.getSlotIndex(), slotPlayerView);
				awayAssignedPlayerIds.add(slot.getPlayer().getId());
			}
		}

		List<PlayerLineupView> homeBench = new ArrayList<>();
		for (var player : playerRepository.findByTeamIdOrderByJerseyNumberAsc(match.getHomeTeam().getId())) {
			if (player == null || player.getId() == null || homeAssignedPlayerIds.contains(player.getId())) {
				continue;
			}
			homeBench.add(new PlayerLineupView(player.getId(), player.getFullName(), player.getJerseyNumber(), player.getPosition(), player.getAvatarUrl()));
		}

		List<PlayerLineupView> awayBench = new ArrayList<>();
		for (var player : playerRepository.findByTeamIdOrderByJerseyNumberAsc(match.getAwayTeam().getId())) {
			if (player == null || player.getId() == null || awayAssignedPlayerIds.contains(player.getId())) {
				continue;
			}
			awayBench.add(new PlayerLineupView(player.getId(), player.getFullName(), player.getJerseyNumber(), player.getPosition(), player.getAvatarUrl()));
		}

		List<LineupSlotPlayerView> homeStarters = new ArrayList<>();
		List<LineupSlotPlayerView> awayStarters = new ArrayList<>();
		for (LineupSlotView slot : pitchSlots) {
			homeStarters.add(homeAssigned.getOrDefault(slot.index(), new LineupSlotPlayerView(slot.index(), slot.label(), null)));
			awayStarters.add(awayAssigned.getOrDefault(slot.index(), new LineupSlotPlayerView(slot.index(), slot.label(), null)));
		}

		TeamLineupView homeTeam = new TeamLineupView(match.getHomeTeam().getName(), homeStarters, homeBench);
		TeamLineupView awayTeam = new TeamLineupView(match.getAwayTeam().getName(), awayStarters, awayBench);

		boolean hasLineup = homeAssignedPlayerIds.size() + awayAssignedPlayerIds.size() > 0;
		String message = hasLineup ? "" : "Admin chưa cập nhật đội hình trận này";
		return new MatchLineupView(
				true,
				message,
				match.getId(),
				match.getRoundName(),
				match.getScheduledAt(),
				displayMatchStatus(match.getStatus()),
				match.getHomeScore(),
				match.getAwayScore(),
				match.getLiveStreamUrl(),
				pitchSlots,
				homeTeam,
				awayTeam
		);
	}

	@Transactional(readOnly = true)
	public TeamSingleLineupView buildTeamLineupView(Long tournamentId, Long teamId) {
		if (tournamentId == null || teamId == null) {
			return TeamSingleLineupView.notFound("Thiếu thông tin đội bóng");
		}
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return TeamSingleLineupView.notFound("Không tìm thấy giải đấu");
		}
		var teamOpt = teamService.findById(teamId);
		if (teamOpt.isEmpty()) {
			return TeamSingleLineupView.notFound("Không tìm thấy đội bóng");
		}
		var team = teamOpt.get();

		boolean belongsToTournament = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId).stream()
				.anyMatch(r -> r != null
						&& r.getStatus() == RegistrationStatus.APPROVED
						&& r.getTeam() != null
						&& teamId.equals(r.getTeam().getId()));
		if (!belongsToTournament) {
			return TeamSingleLineupView.notFound("Đội bóng không thuộc giải đấu này");
		}

		List<LineupSlotView> pitchSlots = buildPitchSlots(tournament.getPitchType());
		List<LineupSlotPlayerView> starters = List.of();

		List<Match> matches = matchService.listByTournamentIdWithDetails(tournamentId).stream()
				.sorted(Comparator
						.comparing((Match m) -> m.getScheduledAt() == null ? LocalDateTime.MIN : m.getScheduledAt())
						.reversed()
						.thenComparing(m -> m.getId() == null ? Long.MIN_VALUE : m.getId(), Comparator.reverseOrder()))
				.toList();

		for (Match match : matches) {
			if (match == null || match.getId() == null) {
				continue;
			}
			List<LineupSlotPlayerView> candidate = matchLineupService.listByMatchId(match.getId()).stream()
					.filter(slot -> slot != null && slot.getPlayer() != null && slot.getPlayer().getTeam() != null
							&& slot.getPlayer().getTeam().getId() != null && teamId.equals(slot.getPlayer().getTeam().getId()))
					.sorted(Comparator.comparing(slot -> slot.getSlotIndex() == null ? Integer.MAX_VALUE : slot.getSlotIndex()))
					.map(slot -> new LineupSlotPlayerView(
							slot.getSlotIndex() == null ? -1 : slot.getSlotIndex(),
							slot.getPosition(),
							new PlayerLineupView(
									slot.getPlayer().getId(),
									slot.getPlayer().getFullName(),
									slot.getPlayer().getJerseyNumber(),
									slot.getPlayer().getPosition(),
									slot.getPlayer().getAvatarUrl()
							)
					))
					.toList();
			if (!candidate.isEmpty()) {
				starters = candidate;
				break;
			}
		}

		Set<Long> starterIds = starters.stream()
				.map(LineupSlotPlayerView::player)
				.map(player -> player == null ? null : player.id())
				.filter(id -> id != null)
				.collect(java.util.stream.Collectors.toSet());

		List<PlayerLineupView> bench = playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId).stream()
				.filter(player -> player != null && player.getId() != null && !starterIds.contains(player.getId()))
				.map(player -> new PlayerLineupView(
						player.getId(),
						player.getFullName(),
						player.getJerseyNumber(),
						player.getPosition(),
						player.getAvatarUrl()
				))
				.toList();

		boolean hasLineup = !starters.isEmpty();
		String message = hasLineup ? "" : "Đội chưa có đội hình được admin thiết lập";
		return new TeamSingleLineupView(true, message, team.getName(), pitchSlots, starters, bench);
	}

	@Transactional(readOnly = true)
	public LiveMatchView buildLiveMatchView(Long tournamentId, Long matchId) {
		if (tournamentId == null || matchId == null) {
			return LiveMatchView.notAvailable("Không tìm thấy trận đấu");
		}
		Match match = matchService.findByIdWithDetails(matchId).orElse(null);
		if (match == null || match.getTournament() == null || match.getTournament().getId() == null
				|| !tournamentId.equals(match.getTournament().getId())) {
			return LiveMatchView.notAvailable("Không tìm thấy trận đấu");
		}
		String homeTeamName = match.getHomeTeam() == null ? "Đội 1" : match.getHomeTeam().getName();
		String awayTeamName = match.getAwayTeam() == null ? "Đội 2" : match.getAwayTeam().getName();
		String title = homeTeamName + " vs " + awayTeamName;
		String liveStreamUrl = match.getLiveStreamUrl() == null ? null : match.getLiveStreamUrl().trim();
		boolean available = liveStreamUrl != null && !liveStreamUrl.isBlank();
		String message = available ? "" : "Trận đấu này chưa có link phát trực tiếp";
		return new LiveMatchView(
				available,
				message,
				match.getId(),
				title,
				homeTeamName,
				awayTeamName,
				liveStreamUrl
		);
	}

	@Transactional(readOnly = true)
	public TournamentStatsView buildTournamentStats(Long tournamentId) {
		if (tournamentId == null) {
			return TournamentStatsView.none();
		}
		var events = matchEventRepository.findByTournamentId(tournamentId);
		Map<Long, PlayerStatRow> scorersMap = new HashMap<>();
		Map<Long, PlayerStatRow> assistsMap = new HashMap<>();
		Map<Long, PlayerStatRow> yellowCardsMap = new HashMap<>();
		Map<Long, PlayerStatRow> redCardsMap = new HashMap<>();

		for (var event : events) {
			if (event.getPlayer() == null || event.getPlayer().getId() == null) continue;
			var player = event.getPlayer();
			var team = player.getTeam();
			String teamName = (team != null) ? team.getName() : "—";

			if (event.getType() == com.example.football_tourament_web.model.enums.MatchEventType.GOAL) {
				scorersMap.computeIfAbsent(player.getId(), id -> new PlayerStatRow(id, player.getFullName(), teamName, player.getAvatarUrl(), 0))
						.increment();
			} else if (event.getType() == com.example.football_tourament_web.model.enums.MatchEventType.ASSIST) {
				assistsMap.computeIfAbsent(player.getId(), id -> new PlayerStatRow(id, player.getFullName(), teamName, player.getAvatarUrl(), 0))
						.increment();
			} else if (event.getType() == com.example.football_tourament_web.model.enums.MatchEventType.YELLOW) {
				yellowCardsMap.computeIfAbsent(player.getId(), id -> new PlayerStatRow(id, player.getFullName(), teamName, player.getAvatarUrl(), 0))
						.increment();
			} else if (event.getType() == com.example.football_tourament_web.model.enums.MatchEventType.RED) {
				redCardsMap.computeIfAbsent(player.getId(), id -> new PlayerStatRow(id, player.getFullName(), teamName, player.getAvatarUrl(), 0))
						.increment();
			}
		}

		var scorers = scorersMap.values().stream().sorted(Comparator.comparingInt(PlayerStatRow::value).reversed()).limit(5).toList();
		var assists = assistsMap.values().stream().sorted(Comparator.comparingInt(PlayerStatRow::value).reversed()).limit(5).toList();
		var yellowCards = yellowCardsMap.values().stream().sorted(Comparator.comparingInt(PlayerStatRow::value).reversed()).limit(5).toList();
		var redCards = redCardsMap.values().stream().sorted(Comparator.comparingInt(PlayerStatRow::value).reversed()).limit(5).toList();

		var tournamentAverages = new TournamentComparisonStats(55, 45, 12, 9, 84, 78, 8, 11);

		return new TournamentStatsView(scorers, assists, yellowCards, redCards, tournamentAverages);
	}

	@Transactional(readOnly = true)
	public ChartsView buildChartsData(Long tournamentId, Long selectedTeamId) {
		if (tournamentId == null) return ChartsView.none();
		var matches = matchService.listByTournamentIdWithDetails(tournamentId);

		Long teamId = selectedTeamId;
		String teamName = null;
		if (teamId == null) {
			List<TeamCardView> teams = buildTournamentTeams(tournamentId);
			if (!teams.isEmpty() && teams.get(0) != null) {
				teamId = teams.get(0).teamId();
				teamName = teams.get(0).teamName();
			}
		}
		if (teamId == null) return ChartsView.none();
		if (teamName == null) {
			teamName = teamService.findById(teamId).map(com.example.football_tourament_web.model.entity.Team::getName).orElse("—");
		}

		final Long teamIdFinal = teamId;
		final String teamNameFinal = teamName;

		var sortedMatches = matches.stream()
				.filter(m -> m != null && m.getHomeTeam() != null && m.getAwayTeam() != null
						&& m.getHomeTeam().getId() != null && m.getAwayTeam().getId() != null)
				.filter(m -> teamIdFinal.equals(m.getHomeTeam().getId()) || teamIdFinal.equals(m.getAwayTeam().getId()))
				.sorted(Comparator
						.comparing((Match m) -> m.getScheduledAt() == null ? LocalDateTime.MAX : m.getScheduledAt())
						.thenComparing(m -> m.getId() == null ? Long.MAX_VALUE : m.getId()))
				.toList();

		int win = 0;
		int draw = 0;
		int loss = 0;
		List<String> matchLabels = new ArrayList<>();
		List<Integer> goalsForByMatch = new ArrayList<>();

		for (Match m : sortedMatches) {
			Integer hs = m.getHomeScore();
			Integer as = m.getAwayScore();
			boolean hasScore = hs != null && as != null;
			if (!hasScore) continue;

			boolean isHome = teamIdFinal.equals(m.getHomeTeam().getId());
			boolean isAway = teamIdFinal.equals(m.getAwayTeam().getId());
			if (!isHome && !isAway) continue;

			int gf = isHome ? hs : as;
			goalsForByMatch.add(gf);

			String round = (m.getRoundName() == null || m.getRoundName().isBlank()) ? "Trận" : m.getRoundName().trim();
			String opponent = isHome ? m.getAwayTeam().getName() : m.getHomeTeam().getName();
			if (opponent == null || opponent.isBlank()) opponent = "—";
			matchLabels.add(round + " vs " + opponent);

			int cmp = Integer.compare(hs, as);
			if (cmp == 0) {
				Integer hp = m.getHomePenalty();
				Integer ap = m.getAwayPenalty();
				if (hp != null && ap != null && !hp.equals(ap)) {
					boolean homeWin = hp > ap;
					boolean teamWin = (isHome && homeWin) || (isAway && !homeWin);
					if (teamWin) win++; else loss++;
				} else {
					draw++;
				}
			} else {
				boolean homeWin = cmp > 0;
				boolean teamWin = (isHome && homeWin) || (isAway && !homeWin);
				if (teamWin) win++; else loss++;
			}
		}

		return new ChartsView(teamIdFinal, teamNameFinal, win, draw, loss, matchLabels, goalsForByMatch);
	}

	private RadarDataset calculateTeamRadarStats(com.example.football_tourament_web.model.entity.Team team, List<Match> allMatches, List<MatchEvent> allEvents) {
		if (team == null) return new RadarDataset("Unknown", List.of(0, 0, 0, 0, 0));

		long teamId = team.getId();
		int attack = 0, defense = 0, discipline = 0;

		attack = (int) allEvents.stream().filter(e -> e.getType() == com.example.football_tourament_web.model.enums.MatchEventType.GOAL && e.getPlayer() != null && e.getPlayer().getTeam() != null && e.getPlayer().getTeam().getId().equals(teamId)).count();

		long goalsConceded = 0;
		for (Match m : allMatches) {
			if (m.getHomeTeam() != null && m.getHomeTeam().getId().equals(teamId) && m.getAwayScore() != null) {
				goalsConceded += m.getAwayScore();
			} else if (m.getAwayTeam() != null && m.getAwayTeam().getId().equals(teamId) && m.getHomeScore() != null) {
				goalsConceded += m.getHomeScore();
			}
		}
		defense = (int) (100 - (goalsConceded * 5));

		long yellow = allEvents.stream().filter(e -> (e.getType() == com.example.football_tourament_web.model.enums.MatchEventType.YELLOW) && e.getPlayer() != null && e.getPlayer().getTeam() != null && e.getPlayer().getTeam().getId().equals(teamId)).count();
		long red = allEvents.stream().filter(e -> (e.getType() == com.example.football_tourament_web.model.enums.MatchEventType.RED) && e.getPlayer() != null && e.getPlayer().getTeam() != null && e.getPlayer().getTeam().getId().equals(teamId)).count();
		discipline = (int) (100 - (yellow * 5) - (red * 10));

		int control = 60 + (attack * 2) - (int) goalsConceded;
		int stamina = 70 + (int) (allMatches.stream().filter(m -> (m.getHomeTeam() != null && m.getHomeTeam().getId().equals(teamId)) || (m.getAwayTeam() != null && m.getAwayTeam().getId().equals(teamId))).count() * 2);

		List<Integer> data = List.of(
				Math.max(0, Math.min(100, attack * 10)),
				Math.max(0, Math.min(100, defense)),
				Math.max(0, Math.min(100, control)),
				Math.max(0, Math.min(100, discipline)),
				Math.max(0, Math.min(100, stamina))
		);
		return new RadarDataset(team.getName(), data);
	}

	@Transactional(readOnly = true)
	public BracketView buildBracketData(Long tournamentId) {
		if (tournamentId == null) return BracketView.none();
		List<Match> allMatches = matchService.listByTournamentIdWithDetails(tournamentId);

		List<Match> knockoutMatches = allMatches.stream()
				.filter(m -> m != null && m.getRoundName() != null)
				.filter(m -> {
					String r = m.getRoundName().trim();
					return !r.startsWith("Bảng ");
				})
				.toList();

		Map<String, String> seedLabels = new HashMap<>();
		knockoutMatches.stream()
				.sorted(Comparator
						.comparing((Match m) -> bracketRoundPriority(m.getRoundName()))
						.thenComparing(m -> m.getId() == null ? Long.MAX_VALUE : m.getId()))
				.forEach(m -> {
					String home = m.getHomeTeam() != null ? m.getHomeTeam().getName() : null;
					String away = m.getAwayTeam() != null ? m.getAwayTeam().getName() : null;
					if (home != null && !home.isBlank() && !seedLabels.containsKey(home)) {
						seedLabels.put(home, "T" + (seedLabels.size() + 1));
					}
					if (away != null && !away.isBlank() && !seedLabels.containsKey(away)) {
						seedLabels.put(away, "T" + (seedLabels.size() + 1));
					}
				});

		Map<String, List<MatchRow>> matchesByRound = new HashMap<>();
		for (Match match : knockoutMatches) {
			String round = match.getRoundName().trim();
			matchesByRound.computeIfAbsent(round, k -> new ArrayList<>()).add(toMatchRow(match));
		}

		List<BracketRound> rounds = matchesByRound.entrySet().stream()
				.sorted(Comparator
						.comparingInt((Map.Entry<String, List<MatchRow>> e) -> bracketRoundPriority(e.getKey()))
						.thenComparing(Map.Entry::getKey))
				.map(e -> {
					var list = new ArrayList<>(e.getValue());
					list.sort(Comparator
							.comparing((MatchRow m) -> m.scheduledAt() == null ? LocalDateTime.MAX : m.scheduledAt())
							.thenComparing(m -> m.id() == null ? Long.MAX_VALUE : m.id()));
					return new BracketRound(e.getKey(), list);
				})
				.toList();

		return new BracketView(seedLabels, rounds);
	}

	private static int bracketRoundPriority(String roundName) {
		if (roundName == null) return 999;
		String r = roundName.trim().toLowerCase();
		if (r.contains("vòng 16") || r.contains("1/8") || r.contains("vòng 1/8")) return 10;
		if (r.contains("tứ kết") || r.contains("quarter")) return 20;
		if (r.contains("bán kết") || r.contains("semi")) return 30;
		if (r.contains("chung kết") || r.contains("final")) return 40;
		if (r.contains("tranh hạng 3") || r.contains("hạng 3")) return 50;
		return 900;
	}

	@Transactional(readOnly = true)
	public ScheduleView buildScheduleView(Long tournamentId) {
		if (tournamentId == null) {
			return ScheduleView.none("Chọn giải đấu để xem lịch thi đấu");
		}

		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return ScheduleView.none("Không tìm thấy giải đấu");
		}

		List<Match> allMatches = matchService.listByTournamentIdWithDetails(tournamentId);
		List<Match> groupMatchesSource = new ArrayList<>();
		List<MatchRow> knockoutMatches = new ArrayList<>();

		for (var match : allMatches) {
			if (match == null || match.getRoundName() == null) {
				continue;
			}
			String roundName = match.getRoundName().trim();
			if (roundName.startsWith("Bảng ")) {
				groupMatchesSource.add(match);
			} else {
				knockoutMatches.add(toMatchRow(match));
			}
		}
		knockoutMatches.sort(Comparator.comparing(m -> m.scheduledAt() == null ? LocalDateTime.MAX : m.scheduledAt()));

		if (tournament.getMode() == TournamentMode.GROUP_STAGE) {
			Map<String, List<GroupTeamRow>> groupTeams = new HashMap<>();
			Map<String, List<MatchRow>> groupMatches = new HashMap<>();
			Map<String, Map<Long, TeamStats>> statsByGroup = new HashMap<>();

			for (String group : List.of("A", "B", "C", "D")) {
				groupTeams.put(group, new ArrayList<>());
				groupMatches.put(group, new ArrayList<>());
				statsByGroup.put(group, new HashMap<>());
			}

			List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
			if (registrations == null) registrations = new ArrayList<>();

			List<Long> seenTeamIds = new ArrayList<>();
			int validTeamCount = 0;
			boolean allAssigned = true;

			for (TournamentRegistration registration : registrations) {
				if (registration == null || registration.getStatus() != RegistrationStatus.APPROVED) {
					continue;
				}
				if (registration.getTeam() == null || registration.getTeam().getId() == null) {
					continue;
				}
				Long teamId = registration.getTeam().getId();
				if (seenTeamIds.contains(teamId)) {
					continue;
				}
				seenTeamIds.add(teamId);
				validTeamCount++;

				String rawGroupName = registration.getGroupName() == null ? "" : registration.getGroupName().trim().toUpperCase();
				String groupName = rawGroupName;

				if (!groupTeams.containsKey(groupName)) {
					if (groupName.contains("A")) groupName = "A";
					else if (groupName.contains("B")) groupName = "B";
					else if (groupName.contains("C")) groupName = "C";
					else if (groupName.contains("D")) groupName = "D";
				}

				if (!groupTeams.containsKey(groupName)) {
					allAssigned = false;
					continue;
				}

				statsByGroup.get(groupName).put(teamId, new TeamStats());
				long memberCount = playerRepository.countByTeamId(teamId);
				groupTeams.get(groupName).add(new GroupTeamRow(teamId, registration.getTeam().getName(), registration.getTeam().getLogoUrl(), memberCount, 0, 0, 0, 0, 0, 0, 0, new ArrayList<>()));
			}

			boolean groupingReady = tournament.getTeamLimit() != null && validTeamCount == tournament.getTeamLimit() && (validTeamCount == 16 || validTeamCount == 8);
			boolean groupingLocked = groupingReady && allAssigned;
			boolean hasGroupMatches = !groupMatchesSource.isEmpty();
			boolean scheduleReady = hasGroupMatches;

			Map<String, List<Match>> groupEntityMatches = new HashMap<>();
			for (String g : List.of("A", "B", "C", "D")) {
				groupEntityMatches.put(g, new ArrayList<>());
			}

			List<Match> groupMatchesSorted = new ArrayList<>(groupMatchesSource);
			groupMatchesSorted.sort(
					Comparator
							.comparing((Match m) -> m.getScheduledAt() == null ? LocalDateTime.MIN : m.getScheduledAt())
							.thenComparing(m -> m.getId() == null ? Long.MIN_VALUE : m.getId())
			);

			for (var match : groupMatchesSorted) {
				String groupName = toGroupName(match.getRoundName());
				if (!groupMatches.containsKey(groupName)) {
					continue;
				}
				groupEntityMatches.get(groupName).add(match);

				if (match.getHomeTeam() != null && match.getHomeTeam().getId() != null) {
					ensureTeamInGroup(groupTeams, statsByGroup, groupName, match.getHomeTeam().getId(), match.getHomeTeam().getName(), match.getHomeTeam().getLogoUrl());
				}
				if (match.getAwayTeam() != null && match.getAwayTeam().getId() != null) {
					ensureTeamInGroup(groupTeams, statsByGroup, groupName, match.getAwayTeam().getId(), match.getAwayTeam().getName(), match.getAwayTeam().getLogoUrl());
				}

				groupMatches.get(groupName).add(toMatchRow(match));

				if (match.getStatus() != MatchStatus.FINISHED || match.getHomeScore() == null || match.getAwayScore() == null) {
					continue;
				}
				if (match.getHomeTeam() == null || match.getAwayTeam() == null || match.getHomeTeam().getId() == null || match.getAwayTeam().getId() == null) {
					continue;
				}
				Map<Long, TeamStats> groupStats = statsByGroup.get(groupName);
				if (groupStats == null) {
					continue;
				}
				TeamStats homeStats = groupStats.get(match.getHomeTeam().getId());
				TeamStats awayStats = groupStats.get(match.getAwayTeam().getId());
				if (homeStats == null || awayStats == null) {
					continue;
				}
				int homeScore = match.getHomeScore();
				int awayScore = match.getAwayScore();
				homeStats.played++;
				awayStats.played++;
				homeStats.goalsFor += homeScore;
				homeStats.goalsAgainst += awayScore;
				awayStats.goalsFor += awayScore;
				awayStats.goalsAgainst += homeScore;
				LocalDateTime stamp = match.getScheduledAt() == null ? LocalDateTime.MIN : match.getScheduledAt();
				if (homeScore > awayScore) {
					homeStats.won++;
					awayStats.lost++;
					homeStats.points += 3;
					homeStats.results.add(new ResultStamp(stamp, "W"));
					awayStats.results.add(new ResultStamp(stamp, "L"));
				} else if (awayScore > homeScore) {
					awayStats.won++;
					homeStats.lost++;
					awayStats.points += 3;
					homeStats.results.add(new ResultStamp(stamp, "L"));
					awayStats.results.add(new ResultStamp(stamp, "W"));
				} else {
					homeStats.drawn++;
					awayStats.drawn++;
					homeStats.points += 1;
					awayStats.points += 1;
					homeStats.results.add(new ResultStamp(stamp, "D"));
					awayStats.results.add(new ResultStamp(stamp, "D"));
				}
			}

			for (String group : List.of("A", "B", "C", "D")) {
				Map<String, Integer> seen = new HashMap<>();
				Map<Long, String> legById = new HashMap<>();
				for (Match m : groupEntityMatches.get(group)) {
					if (m == null || m.getHomeTeam() == null || m.getAwayTeam() == null) continue;
					Long hid = m.getHomeTeam().getId();
					Long aid = m.getAwayTeam().getId();
					if (hid == null || aid == null) continue;
					long a = Math.min(hid, aid);
					long b = Math.max(hid, aid);
					String key = a + ":" + b;
					int cnt = seen.getOrDefault(key, 0);
					legById.put(m.getId(), cnt == 0 ? "L1" : "L2");
					seen.put(key, cnt + 1);
				}

				List<MatchRow> replaced = new ArrayList<>();
				for (MatchRow mr : groupMatches.get(group)) {
					String leg = legById.getOrDefault(mr.id(), "");
					replaced.add(new MatchRow(
							mr.id(), mr.roundName(), mr.scheduledAt(),
							mr.homeTeamName(), mr.awayTeamName(),
							mr.homeScore(), mr.awayScore(),
							mr.homePenalty(), mr.awayPenalty(),
							leg,
							mr.statusLabel(),
							mr.liveStreamUrl(),
							mr.liveArchiveUrl()
					));
				}
				groupMatches.put(group, replaced);

				List<GroupTeamRow> rows = new ArrayList<>();
				for (GroupTeamRow row : groupTeams.get(group)) {
					TeamStats stats = statsByGroup.get(group).getOrDefault(row.teamId(), new TeamStats());
					List<ResultStamp> sortedResults = new ArrayList<>(stats.results);
					sortedResults.sort(Comparator
							.comparing((ResultStamp r) -> r.time() == null ? LocalDateTime.MIN : r.time())
							.thenComparing(r -> r.result() == null ? "" : r.result()));
					List<String> last5Form = sortedResults.stream()
							.map(ResultStamp::result)
							.toList();
					if (last5Form.size() > 5) {
						last5Form = last5Form.subList(last5Form.size() - 5, last5Form.size());
					}
					rows.add(new GroupTeamRow(row.teamId(), row.name(), row.logoUrl(), row.memberCount(), stats.played, stats.won, stats.drawn, stats.lost, stats.points, stats.goalsFor, stats.goalsAgainst, last5Form));
				}
				rows = sortByGroupRules(rows, groupEntityMatches.get(group));
				groupTeams.put(group, rows);
				groupMatches.get(group).sort(Comparator.comparing(m -> m.scheduledAt() == null ? LocalDateTime.MAX : m.scheduledAt()));
			}

			String message = scheduleReady ? "" : "Giải đấu chưa chia bảng nên chưa có lịch thi đấu";
			return new ScheduleView(scheduleReady, "GROUP_STAGE", message, groupingLocked, groupTeams, groupMatches, knockoutMatches);
		}

		boolean scheduleReady = !knockoutMatches.isEmpty();
		String message = scheduleReady ? "" : "Giải đấu chưa chia cặp nên chưa có lịch thi đấu";
		return new ScheduleView(scheduleReady, "KNOCKOUT", message, false, Map.of(), Map.of(), knockoutMatches);
	}

	@Transactional(readOnly = true)
	public LiveArchiveView buildLiveArchiveView(Long tournamentId) {
		if (tournamentId == null) {
			return LiveArchiveView.none("Chọn giải đấu để xem video live đã lưu");
		}
		Tournament tournament = tournamentService.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return LiveArchiveView.none("Không tìm thấy giải đấu");
		}
		List<LiveArchiveRow> rows = new ArrayList<>();
		for (Match match : matchService.listByTournamentIdWithDetails(tournamentId)) {
			if (match == null || match.getStatus() != MatchStatus.FINISHED) {
				continue;
			}
			String archiveUrl = match.getLiveArchiveUrl();
			if (archiveUrl == null || archiveUrl.isBlank()) {
				continue;
			}
			String homeName = match.getHomeTeam() == null ? "Đội 1" : match.getHomeTeam().getName();
			String awayName = match.getAwayTeam() == null ? "Đội 2" : match.getAwayTeam().getName();
			rows.add(new LiveArchiveRow(
					match.getId(),
					homeName + " vs " + awayName,
					homeName,
					awayName,
					match.getRoundName(),
					match.getScheduledAt(),
					archiveUrl
			));
		}
		rows.sort(Comparator.comparing((LiveArchiveRow r) -> r.scheduledAt() == null ? LocalDateTime.MIN : r.scheduledAt()).reversed());
		if (rows.isEmpty()) {
			return LiveArchiveView.none("Chưa có video live nào được lưu");
		}
		return new LiveArchiveView(true, "", rows);
	}

	private String toGroupName(String roundName) {
		if (roundName == null) return "";
		String value = roundName.trim().toUpperCase();
		if (value.contains("BẢNG A")) return "A";
		if (value.contains("BẢNG B")) return "B";
		if (value.contains("BẢNG C")) return "C";
		if (value.contains("BẢNG D")) return "D";
		return "";
	}

	private List<LineupSlotView> buildPitchSlots(PitchType pitchType) {
		String normalized = pitchType == null ? "" : pitchType.name();
		if ("PITCH_5".equalsIgnoreCase(normalized)) {
			return List.of(
					new LineupSlotView(0, 15, 30, "FW"),
					new LineupSlotView(1, 15, 70, "FW"),
					new LineupSlotView(2, 60, 30, "DF"),
					new LineupSlotView(3, 60, 70, "DF"),
					new LineupSlotView(4, 82, 50, "GK")
			);
		}
		return List.of(
				new LineupSlotView(0, 12, 25, "FW"),
				new LineupSlotView(1, 12, 50, "FW"),
				new LineupSlotView(2, 12, 75, "FW"),
				new LineupSlotView(3, 58, 25, "DF"),
				new LineupSlotView(4, 58, 50, "DF"),
				new LineupSlotView(5, 58, 75, "DF"),
				new LineupSlotView(6, 82, 50, "GK")
		);
	}

	private MatchRow toMatchRow(Match match) {
		return new MatchRow(
				match.getId(),
				match.getRoundName(),
				match.getScheduledAt(),
				match.getHomeTeam() == null ? "—" : match.getHomeTeam().getName(),
				match.getAwayTeam() == null ? "—" : match.getAwayTeam().getName(),
				match.getHomeScore(),
				match.getAwayScore(),
				match.getHomePenalty(),
				match.getAwayPenalty(),
				null,
				displayMatchStatus(match.getStatus()),
				match.getLiveStreamUrl(),
				match.getLiveArchiveUrl()
		);
	}

	private String displayMatchStatus(MatchStatus status) {
		if (status == null) return "Đang cập nhật";
		return switch (status) {
			case FINISHED -> "Hoàn thành";
			case LIVE -> "Đang diễn ra";
			case SCHEDULED -> "Sắp diễn ra";
		};
	}

	private void ensureTeamInGroup(
			Map<String, List<GroupTeamRow>> groupTeams,
			Map<String, Map<Long, TeamStats>> statsByGroup,
			String groupName,
			Long teamId,
			String teamName,
			String logoUrl
	) {
		if (groupName == null || !groupTeams.containsKey(groupName) || teamId == null) {
			return;
		}
		List<GroupTeamRow> rows = groupTeams.get(groupName);
		boolean exists = rows.stream().anyMatch(row -> row != null && teamId.equals(row.teamId()));
		if (!exists) {
			long memberCount = playerRepository.countByTeamId(teamId);
			rows.add(new GroupTeamRow(teamId, teamName, logoUrl, memberCount, 0, 0, 0, 0, 0, 0, 0, new ArrayList<>()));
		}
		statsByGroup.computeIfAbsent(groupName, key -> new HashMap<>()).putIfAbsent(teamId, new TeamStats());
	}

	private com.example.football_tourament_web.model.entity.AppUser requireCurrentUser(Authentication authentication) {
		String email = authentication == null ? null : authentication.getName();
		if (email == null || email.isBlank()) {
			return null;
		}
		return userService.findByEmail(email).orElse(null);
	}

	private static final class TeamStats {
		private int played;
		private int won;
		private int drawn;
		private int lost;
		private int points;
		private int goalsFor;
		private int goalsAgainst;
		private List<ResultStamp> results = new ArrayList<>();
	}

	private record ResultStamp(LocalDateTime time, String result) {
	}

	private List<GroupTeamRow> sortByGroupRules(List<GroupTeamRow> rows, List<Match> groupMatches) {
		if (rows == null || rows.size() <= 1) return rows;
		List<GroupTeamRow> sorted = new ArrayList<>(rows);
		sorted.sort(
				Comparator.comparingInt(GroupTeamRow::points).reversed()
						.thenComparing(Comparator.comparingInt(GroupTeamRow::goalDiff).reversed())
						.thenComparing(Comparator.comparingInt(GroupTeamRow::goalsFor).reversed())
						.thenComparing(GroupTeamRow::name, String.CASE_INSENSITIVE_ORDER)
		);

		int i = 0;
		while (i < sorted.size()) {
			int j = i + 1;
			while (j < sorted.size()
					&& sorted.get(j).points() == sorted.get(i).points()
					&& sorted.get(j).goalDiff() == sorted.get(i).goalDiff()
					&& sorted.get(j).goalsFor() == sorted.get(i).goalsFor()) {
				j++;
			}
			if (j - i >= 2) {
				List<GroupTeamRow> block = new ArrayList<>(sorted.subList(i, j));
				List<GroupTeamRow> resolved = sortTieBlockByHeadToHeadGoalDiff(block, groupMatches);
				for (int k = 0; k < resolved.size(); k++) {
					sorted.set(i + k, resolved.get(k));
				}
			}
			i = j;
		}
		return sorted;
	}

	private List<GroupTeamRow> sortTieBlockByHeadToHeadGoalDiff(List<GroupTeamRow> block, List<Match> groupMatches) {
		if (block == null || block.size() <= 1) return block;
		java.util.Set<Long> ids = block.stream().map(GroupTeamRow::teamId).collect(java.util.stream.Collectors.toSet());
		java.util.Map<Long, H2H> h2h = new java.util.HashMap<>();
		for (Long id : ids) h2h.put(id, new H2H());

		if (groupMatches != null) {
			for (Match m : groupMatches) {
				if (m == null || m.getStatus() != MatchStatus.FINISHED) continue;
				if (m.getHomeTeam() == null || m.getAwayTeam() == null) continue;
				Long hid = m.getHomeTeam().getId();
				Long aid = m.getAwayTeam().getId();
				if (hid == null || aid == null) continue;
				if (!ids.contains(hid) || !ids.contains(aid)) continue;
				Integer hs = m.getHomeScore();
				Integer as = m.getAwayScore();
				if (hs == null || as == null) continue;

				h2h.get(hid).gf += hs;
				h2h.get(hid).ga += as;
				h2h.get(aid).gf += as;
				h2h.get(aid).ga += hs;
			}
		}

		List<GroupTeamRow> resolved = new ArrayList<>(block);
		resolved.sort(Comparator
				.comparingInt((GroupTeamRow r) -> h2h.getOrDefault(r.teamId(), new H2H()).gd()).reversed()
				.thenComparingInt((GroupTeamRow r) -> h2h.getOrDefault(r.teamId(), new H2H()).gf).reversed()
				.thenComparing(GroupTeamRow::name, String.CASE_INSENSITIVE_ORDER));
		return resolved;
	}

	private static final class H2H {
		int gf;
		int ga;
		int gd() { return gf - ga; }
	}

	public record PlayerPrefill(String fullName, Integer jerseyNumber, String avatarUrl) {
	}

	public record TeamPrefillResponse(
			boolean found,
			String message,
			Long teamId,
			String teamName,
			String representativeName,
			String contactEmail,
			String contactPhone,
			String logoUrl,
			List<PlayerPrefill> players
	) {
		public static TeamPrefillResponse found(
				Long teamId,
				String teamName,
				String representativeName,
				String contactEmail,
				String contactPhone,
				String logoUrl,
				List<PlayerPrefill> players
		) {
			return new TeamPrefillResponse(
					true,
					null,
					teamId,
					teamName,
					representativeName,
					contactEmail,
					contactPhone,
					logoUrl,
					players
			);
		}

		public static TeamPrefillResponse notFound(String message) {
			return new TeamPrefillResponse(false, message, null, null, null, null, null, null, List.of());
		}
	}

	public record SignUpTeamOption(Long id, String name) {
	}

	public record SignUpTeamPrefill(
			Long teamId,
			String teamName,
			String representativeName,
			String contactEmail,
			String contactPhone,
			String logoUrl,
			List<PlayerPrefill> players
	) {
	}

	public record RegistrationSubmitResult(boolean success, String message) {
		public static RegistrationSubmitResult success(String message) {
			return new RegistrationSubmitResult(true, message);
		}

		public static RegistrationSubmitResult failed(String message) {
			return new RegistrationSubmitResult(false, message);
		}
	}

	public record PaymentInfo(BigDecimal registrationFee, BigDecimal walletBalance, String registrationFeeText, String walletBalanceText, boolean hasEnoughBalance) {
	}

	public record GroupTeamRow(Long teamId, String name, String logoUrl, long memberCount, int played, int won, int drawn, int lost, int points, int goalsFor, int goalsAgainst, List<String> form) {
		public int goalDiff() {
			return goalsFor - goalsAgainst;
		}

		public double formIndex() {
			if (form == null || form.isEmpty()) return 0.0;
			int n = form.size();
			double sum = 0;
			for (int i = 0; i < n; i++) {
				String r = form.get(i);
				int pts = "W".equalsIgnoreCase(r) ? 3 : ("D".equalsIgnoreCase(r) ? 1 : 0);
				int weight = i + 1;
				sum += pts * weight;
			}
			return sum / n;
		}
	}

	public static class PlayerStatRow {
		private final Long id;
		private final String name;
		private final String teamName;
		private final String avatarUrl;
		private int value;

		public PlayerStatRow(Long id, String name, String teamName, String avatarUrl, int value) {
			this.id = id;
			this.name = name;
			this.teamName = teamName;
			this.avatarUrl = avatarUrl;
			this.value = value;
		}

		public void increment() {
			this.value++;
		}

		public Long id() { return id; }
		public String name() { return name; }
		public String teamName() { return teamName; }
		public String avatarUrl() { return avatarUrl; }
		public int value() { return value; }
	}

	public record TournamentComparisonStats(
			int homePossession, int awayPossession,
			int homeShotsOnTarget, int awayShotsOnTarget,
			int homePassAccuracy, int awayPassAccuracy,
			int homeFouls, int awayFouls
	) {}

	public record TournamentStatsView(
			List<PlayerStatRow> scorers,
			List<PlayerStatRow> assists,
			List<PlayerStatRow> yellowCards,
			List<PlayerStatRow> redCards,
			TournamentComparisonStats averages
	) {
		public static TournamentStatsView none() {
			return new TournamentStatsView(List.of(), List.of(), List.of(), List.of(), new TournamentComparisonStats(50, 50, 0, 0, 0, 0, 0, 0));
		}
	}

	public static final class ChartsView {
		private final Long teamId;
		private final String teamName;
		private final int win;
		private final int draw;
		private final int loss;
		private final List<String> matchLabels;
		private final List<Integer> goalsForByMatch;

		public ChartsView(Long teamId, String teamName, int win, int draw, int loss, List<String> matchLabels, List<Integer> goalsForByMatch) {
			this.teamId = teamId;
			this.teamName = teamName;
			this.win = win;
			this.draw = draw;
			this.loss = loss;
			this.matchLabels = matchLabels == null ? List.of() : matchLabels;
			this.goalsForByMatch = goalsForByMatch == null ? List.of() : goalsForByMatch;
		}

		public static ChartsView none() {
			return new ChartsView(null, null, 0, 0, 0, List.of(), List.of());
		}

		public Long getTeamId() {
			return teamId;
		}

		public String getTeamName() {
			return teamName;
		}

		public int getWin() {
			return win;
		}

		public int getDraw() {
			return draw;
		}

		public int getLoss() {
			return loss;
		}

		public List<String> getMatchLabels() {
			return matchLabels;
		}

		public List<Integer> getGoalsForByMatch() {
			return goalsForByMatch;
		}
	}

	public record RadarDataset(String label, List<Integer> data) {}

	public record BracketRound(String roundName, List<MatchRow> matches) {}

	public record BracketView(Map<String, String> seedLabels, List<BracketRound> rounds) {
		public static BracketView none() {
			return new BracketView(Map.of(), List.of());
		}
	}

	public record MatchRow(
			Long id,
			String roundName,
			LocalDateTime scheduledAt,
			String homeTeamName,
			String awayTeamName,
			Integer homeScore,
			Integer awayScore,
			Integer homePenalty,
			Integer awayPenalty,
			String legLabel,
			String statusLabel,
			String liveStreamUrl,
			String liveArchiveUrl
	) {
		public String homeDisplayScore() {
			if (homeScore == null) return "-";
			if (awayScore != null && homeScore.equals(awayScore) && homePenalty != null && awayPenalty != null) {
				return homeScore + " (" + homePenalty + ")";
			}
			return String.valueOf(homeScore);
		}

		public String awayDisplayScore() {
			if (awayScore == null) return "-";
			if (homeScore != null && homeScore.equals(awayScore) && homePenalty != null && awayPenalty != null) {
				return awayScore + " (" + awayPenalty + ")";
			}
			return String.valueOf(awayScore);
		}

		public String scoreLabel() {
			if (homeScore == null || awayScore == null) return "";
			if (homeScore.equals(awayScore) && homePenalty != null && awayPenalty != null) {
				return homeScore + " : " + awayScore + " (P: " + homePenalty + " - " + awayPenalty + ")";
			}
			return homeScore + " : " + awayScore;
		}

		public boolean decidedByPenalties() {
			return homeScore != null && awayScore != null
					&& homeScore.equals(awayScore)
					&& homePenalty != null && awayPenalty != null;
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

	public record ScheduleView(
			boolean scheduleReady,
			String scheduleMode,
			String scheduleMessage,
			boolean groupingLocked,
			Map<String, List<GroupTeamRow>> groupTeams,
			Map<String, List<MatchRow>> groupMatches,
			List<MatchRow> knockoutMatches
	) {
		public static ScheduleView none(String message) {
			return new ScheduleView(false, "NONE", message, false, Map.of(), Map.of(), List.of());
		}
	}

	public record LiveArchiveRow(
			Long matchId,
			String title,
			String homeTeamName,
			String awayTeamName,
			String roundName,
			LocalDateTime scheduledAt,
			String archiveUrl
	) {
	}

	public record LiveArchiveView(
			boolean hasVideos,
			String message,
			List<LiveArchiveRow> videos
	) {
		public static LiveArchiveView none(String message) {
			return new LiveArchiveView(false, message, List.of());
		}
	}

	public record LineupSlotView(int index, int top, int left, String label) {
	}

	public record PlayerLineupView(
			Long id,
			String fullName,
			Integer jerseyNumber,
			String position,
			String avatarUrl
	) {
	}

	public record LineupSlotPlayerView(
			int slotIndex,
			String position,
			PlayerLineupView player
	) {
	}

	public record TeamLineupView(
			String teamName,
			List<LineupSlotPlayerView> starters,
			List<PlayerLineupView> bench
	) {
	}

	public record MatchLineupView(
			boolean found,
			String message,
			Long matchId,
			String roundName,
			LocalDateTime scheduledAt,
			String statusLabel,
			Integer homeScore,
			Integer awayScore,
			String liveStreamUrl,
			List<LineupSlotView> pitchSlots,
			TeamLineupView homeTeam,
			TeamLineupView awayTeam
	) {
		public static MatchLineupView notFound(String message) {
			return new MatchLineupView(false, message, null, null, null, null, null, null, null, List.of(), null, null);
		}
	}

	public record LiveMatchView(
			boolean available,
			String message,
			Long matchId,
			String title,
			String homeTeamName,
			String awayTeamName,
			String liveStreamUrl
	) {
		public static LiveMatchView notAvailable(String message) {
			return new LiveMatchView(false, message, null, "Phát trực tiếp", "Đội 1", "Đội 2", null);
		}
	}

	public record TeamCardView(
			Long teamId,
			String teamName,
			String captainName,
			String teamCode,
			long memberCount,
			String coverLetter,
			String logoUrl
	) {
	}

	public record TeamSingleLineupView(
			boolean found,
			String message,
			String teamName,
			List<LineupSlotView> pitchSlots,
			List<LineupSlotPlayerView> starters,
			List<PlayerLineupView> bench
	) {
		public static TeamSingleLineupView notFound(String message) {
			return new TeamSingleLineupView(false, message, null, List.of(), List.of(), List.of());
		}
	}
}

