package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.model.enums.UserRole;
import com.example.football_tourament_web.model.enums.UserStatus;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.core.TeamService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TransactionService;
import com.example.football_tourament_web.service.core.UserService;
import com.example.football_tourament_web.service.common.ViewFormatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class AdminUserManagementService {
	private final UserService userService;
	private final TeamService teamService;
	private final PlayerRepository playerRepository;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final TransactionService transactionService;
	private final ViewFormatService viewFormatService;

	public AdminUserManagementService(
			UserService userService,
			TeamService teamService,
			PlayerRepository playerRepository,
			TournamentRegistrationService tournamentRegistrationService,
			TransactionService transactionService,
			ViewFormatService viewFormatService
	) {
		this.userService = userService;
		this.teamService = teamService;
		this.playerRepository = playerRepository;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.transactionService = transactionService;
		this.viewFormatService = viewFormatService;
	}

	@Transactional(readOnly = true)
	public List<AppUser> listUsers() {
		return userService.listUsersByRole(UserRole.USER);
	}

	@Transactional(readOnly = true)
	public UserDetailView getUserDetail(Long userId) {
		if (userId == null) return null;
		AppUser user = userService.findById(userId).orElse(null);
		if (user == null) return null;
		Team team = teamService.findCaptainTeam(userId).orElse(null);
		String teamName = team == null ? null : team.getName();
		return new UserDetailView(
				user,
				viewFormatService.formatDate(user.getCreatedAt()),
				displayUserRole(user.getRole()),
				displayUserStatus(user.getStatus()),
				user.getStatus() == UserStatus.LOCKED,
				teamName == null || teamName.isBlank() ? "Chưa có đội" : teamName,
				teamName != null && !teamName.isBlank()
		);
	}

	@Transactional
	public void toggleLock(Long userId) {
		if (userId == null) return;
		AppUser user = userService.findById(userId).orElse(null);
		if (user == null) return;
		UserStatus next = user.getStatus() == UserStatus.LOCKED ? UserStatus.ACTIVE : UserStatus.LOCKED;
		userService.updateStatus(userId, next);
	}

	@Transactional(readOnly = true)
	public UserTeamDetailView getUserTeamDetail(Long userId, Long teamId) {
		List<Team> teams = userId == null ? List.of() : teamService.listByCaptainWithCaptain(userId);

		Team selected = null;
		if (teamId != null) {
			selected = teamService.findByIdWithCaptain(teamId).orElse(null);
		} else if (!teams.isEmpty()) {
			selected = teams.get(0);
		}

		if (selected == null || selected.getId() == null) {
			return new UserTeamDetailView(
					userId,
					teams,
					null,
					"Chưa có đội",
					"—",
					"—",
					"—",
					0,
					0,
					null,
					List.of()
			);
		}

		String captainName = selected.getCaptain() == null ? null : selected.getCaptain().getFullName();
		long memberCount = playerRepository.countByTeamId(selected.getId());
		List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(selected.getId());

		long tournamentCount = 0;
		var approvedRegs = tournamentRegistrationService.listApprovedByTeamId(selected.getId());
		if (approvedRegs != null && !approvedRegs.isEmpty()) {
			var seenTournamentIds = new HashSet<Long>();
			for (var r : approvedRegs) {
				if (r == null || r.getTournament() == null || r.getTournament().getId() == null) continue;
				seenTournamentIds.add(r.getTournament().getId());
			}
			tournamentCount = seenTournamentIds.size();
		}

		return new UserTeamDetailView(
				userId,
				teams,
				selected.getId(),
				selected.getName(),
				"Đang hoạt động",
				captainName == null || captainName.isBlank() ? "Chưa cập nhật" : captainName,
				viewFormatService.formatDate(selected.getCreatedAt()),
				memberCount,
				tournamentCount,
				selected.getLogoUrl(),
				members
		);
	}

	@Transactional(readOnly = true)
	public List<AdminTransactionRow> listUserTransactions(Long userId) {
		if (userId == null) return List.of();
		return transactionService.listByUserId(userId).stream()
				.map(t -> {
					String code = t == null ? null : t.getCode();
					String description = t == null ? null : t.getDescription();
					String amountText = viewFormatService.formatMoney(t == null ? null : t.getAmount());
					String timeText = viewFormatService.formatDateTime(t == null ? null : t.getCreatedAt());
					TransactionStatus status = t == null ? null : t.getStatus();
					String statusLabel = transactionStatusLabel(status);
					String statusClass = transactionStatusClass(status);
					return new AdminTransactionRow(code, description, amountText, timeText, statusLabel, statusClass);
				})
				.toList();
	}

	private String displayUserRole(UserRole role) {
		if (role == null) return "—";
		return switch (role) {
			case ADMIN -> "Admin";
			case USER -> "User";
		};
	}

	private String displayUserStatus(UserStatus status) {
		if (status == null) return "—";
		return switch (status) {
			case ACTIVE -> "Đang kích hoạt";
			case LOCKED -> "Đã khóa";
		};
	}

	private String transactionStatusLabel(TransactionStatus status) {
		if (status == null) return "Đang chờ";
		return switch (status) {
			case SUCCESS -> "Thành công";
			case FAILED -> "Thất bại";
			case PENDING -> "Đang chờ";
		};
	}

	private String transactionStatusClass(TransactionStatus status) {
		if (status == null) return "status--pending";
		return switch (status) {
			case SUCCESS -> "status--success";
			case FAILED -> "status--failed";
			case PENDING -> "status--pending";
		};
	}

	public static final class UserDetailView {
		private final AppUser user;
		private final String registeredAt;
		private final String roleLabel;
		private final String statusLabel;
		private final boolean locked;
		private final String teamName;
		private final boolean hasTeam;

		public UserDetailView(AppUser user, String registeredAt, String roleLabel, String statusLabel, boolean locked, String teamName, boolean hasTeam) {
			this.user = user;
			this.registeredAt = registeredAt;
			this.roleLabel = roleLabel;
			this.statusLabel = statusLabel;
			this.locked = locked;
			this.teamName = teamName;
			this.hasTeam = hasTeam;
		}

		public AppUser getUser() { return user; }
		public String getRegisteredAt() { return registeredAt; }
		public String getRoleLabel() { return roleLabel; }
		public String getStatusLabel() { return statusLabel; }
		public boolean isLocked() { return locked; }
		public String getTeamName() { return teamName; }
		public boolean isHasTeam() { return hasTeam; }
	}

	public static final class UserTeamDetailView {
		private final Long userId;
		private final List<Team> teams;
		private final Long selectedTeamId;
		private final String teamName;
		private final String teamStatus;
		private final String captainName;
		private final String createdAt;
		private final long memberCount;
		private final long tournamentCount;
		private final String teamLogoUrl;
		private final List<Player> members;

		public UserTeamDetailView(Long userId, List<Team> teams, Long selectedTeamId, String teamName, String teamStatus, String captainName, String createdAt, long memberCount, long tournamentCount, String teamLogoUrl, List<Player> members) {
			this.userId = userId;
			this.teams = teams;
			this.selectedTeamId = selectedTeamId;
			this.teamName = teamName;
			this.teamStatus = teamStatus;
			this.captainName = captainName;
			this.createdAt = createdAt;
			this.memberCount = memberCount;
			this.tournamentCount = tournamentCount;
			this.teamLogoUrl = teamLogoUrl;
			this.members = members;
		}

		public Long getUserId() { return userId; }
		public List<Team> getTeams() { return teams; }
		public Long getSelectedTeamId() { return selectedTeamId; }
		public String getTeamName() { return teamName; }
		public String getTeamStatus() { return teamStatus; }
		public String getCaptainName() { return captainName; }
		public String getCreatedAt() { return createdAt; }
		public long getMemberCount() { return memberCount; }
		public long getTournamentCount() { return tournamentCount; }
		public String getTeamLogoUrl() { return teamLogoUrl; }
		public List<Player> getMembers() { return members; }
	}

	public static final class AdminTransactionRow {
		private final String code;
		private final String description;
		private final String amountText;
		private final String timeText;
		private final String statusLabel;
		private final String statusClass;

		public AdminTransactionRow(String code, String description, String amountText, String timeText, String statusLabel, String statusClass) {
			this.code = code;
			this.description = description;
			this.amountText = amountText;
			this.timeText = timeText;
			this.statusLabel = statusLabel;
			this.statusClass = statusClass;
		}

		public String getCode() { return code; }
		public String getDescription() { return description; }
		public String getAmountText() { return amountText; }
		public String getTimeText() { return timeText; }
		public String getStatusLabel() { return statusLabel; }
		public String getStatusClass() { return statusClass; }
	}
}
