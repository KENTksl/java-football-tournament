package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.RegistrationStatus;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TournamentService;
import com.example.football_tourament_web.service.common.ViewFormatService;
import com.example.football_tourament_web.service.core.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdminTeamManagementService {
	private final TournamentService tournamentService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final PlayerRepository playerRepository;
	private final ViewFormatService viewFormatService;
	private final TransactionService transactionService;

	public AdminTeamManagementService(
			TournamentService tournamentService,
			TournamentRegistrationService tournamentRegistrationService,
			PlayerRepository playerRepository,
			ViewFormatService viewFormatService,
			TransactionService transactionService
	) {
		this.tournamentService = tournamentService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.playerRepository = playerRepository;
		this.viewFormatService = viewFormatService;
		this.transactionService = transactionService;
	}

	public TeamManagementView buildTeamManagementView(Long tournamentId, String status, String search, int page, int size) {
		List<Tournament> tournaments = tournamentService.listTournaments();
		Long selectedTournamentId = tournamentId;
		if (selectedTournamentId == null && tournaments != null && !tournaments.isEmpty()) {
			selectedTournamentId = tournaments.get(0).getId();
		}

		RegistrationStatus selectedStatus = parseRegistrationStatus(status);
		String selectedStatusText = selectedStatus == null ? "ALL" : selectedStatus.name();

		List<TeamRegistrationRow> allRows = buildTeamRegistrationRows(selectedTournamentId, selectedStatus, search);
		PagedResult<TeamRegistrationRow> paged = paginate(allRows, page, size);

		return new TeamManagementView(
				tournaments == null ? List.of() : tournaments,
				selectedTournamentId,
				selectedStatusText,
				paged.getItems(),
				paged.getCurrentPage(),
				paged.getTotalPages(),
				search
		);
	}

	@Transactional
	public UpdateStatusResult updateRegistrationStatus(Long registrationId, Long tournamentId, String targetStatus) {
		RegistrationStatus newStatus = parseRegistrationStatus(targetStatus);
		if (newStatus == null) {
			return new UpdateStatusResult(false, "Trạng thái cập nhật không hợp lệ");
		}
		if (registrationId == null || tournamentId == null) {
			return new UpdateStatusResult(false, "Không tìm thấy hồ sơ đăng ký cần cập nhật");
		}

		var registration = tournamentRegistrationService.findByIdWithDetails(registrationId).orElse(null);
		if (registration == null || registration.getTournament() == null || registration.getTournament().getId() == null
				|| !registration.getTournament().getId().equals(tournamentId)) {
			return new UpdateStatusResult(false, "Không tìm thấy hồ sơ đăng ký cần cập nhật");
		}

		if (newStatus == RegistrationStatus.REJECTED) {
			var paid = registration.getPaidAmount() == null ? BigDecimal.ZERO : registration.getPaidAmount();
			boolean shouldRefund = paid.signum() > 0 && registration.getRefundTransactionCode() == null;
			if (shouldRefund) {
				if (registration.getRegisteredBy() == null || registration.getRegisteredBy().getId() == null) {
					return new UpdateStatusResult(false, "Không thể hoàn phí vì thiếu thông tin người đăng ký");
				}

				String txCode = "REFUND_REG_FEE_" + tournamentId + "_" + UUID.randomUUID();
				Transaction refundTx = new Transaction(
						txCode,
						"Hoàn phí đăng ký giải đấu: " + (registration.getTournament().getName() == null ? ("#" + tournamentId) : registration.getTournament().getName()),
						paid,
						registration.getRegisteredBy()
				);
				refundTx.setStatus(TransactionStatus.SUCCESS);
				transactionService.save(refundTx);

				var tournament = registration.getTournament();
				var pool = tournament.getPrizePool() == null ? BigDecimal.ZERO : tournament.getPrizePool();
				BigDecimal updatedPool = pool.subtract(paid);
				tournament.setPrizePool(updatedPool.signum() < 0 ? BigDecimal.ZERO : updatedPool);
				tournamentService.save(tournament);

				registration.setRefundedAmount(paid);
				registration.setRefundTransactionCode(txCode);
				registration.setRefundedAt(Instant.now());
			}
		}

		registration.setStatus(newStatus);
		if (newStatus != RegistrationStatus.APPROVED) {
			registration.setGroupName(null);
		}
		tournamentRegistrationService.save(registration);
		if (newStatus == RegistrationStatus.REJECTED) {
			var paid = registration.getPaidAmount() == null ? BigDecimal.ZERO : registration.getPaidAmount();
			if (paid.signum() > 0) {
				return new UpdateStatusResult(true, "Đã từ chối hồ sơ và hoàn phí: " + viewFormatService.formatMoney(paid));
			}
		}
		return new UpdateStatusResult(true, "Đã cập nhật trạng thái hồ sơ");
	}

	public TeamDetailView buildTeamDetailView(Long registrationId) {
		if (registrationId == null) return null;
		var registration = tournamentRegistrationService.findByIdWithDetails(registrationId).orElse(null);
		if (registration == null || registration.getTeam() == null || registration.getTeam().getId() == null) return null;

		Team team = registration.getTeam();
		Long teamId = team.getId();
		Long tournamentId = registration.getTournament() == null ? null : registration.getTournament().getId();

		long memberCount = playerRepository.countByTeamId(teamId);
		List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId);

		String captainName = team.getCaptain() == null ? "Chưa cập nhật" : (team.getCaptain().getFullName() == null ? "Chưa cập nhật" : team.getCaptain().getFullName());
		String captainPhone = team.getCaptain() == null || team.getCaptain().getPhone() == null ? "Chưa cập nhật" : team.getCaptain().getPhone();

		RegistrationStatus status = registration.getStatus();
		String statusLabel = displayRegistrationStatus(status);
		String statusClass = status == RegistrationStatus.APPROVED ? "badge--approved" : (status == RegistrationStatus.REJECTED ? "badge--rejected" : "badge--pending");

		return new TeamDetailView(
				registrationId,
				tournamentId,
				viewFormatService.formatDate(registration.getCreatedAt()),
				statusLabel,
				statusClass,
				status == RegistrationStatus.PENDING,
				team.getName(),
				captainName,
				captainPhone,
				team.getLogoUrl(),
				viewFormatService.formatDate(team.getCreatedAt()),
				memberCount,
				members == null ? List.of() : members
		);
	}

	private String displayRegistrationStatus(RegistrationStatus status) {
		if (status == null) return "Không xác định";
		return switch (status) {
			case PENDING -> "Chờ duyệt";
			case APPROVED -> "Đã duyệt";
			case REJECTED -> "Đã hủy";
		};
	}

	private RegistrationStatus parseRegistrationStatus(String raw) {
		if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
			return null;
		}
		try {
			return RegistrationStatus.valueOf(raw.toUpperCase());
		} catch (Exception ex) {
			return null;
		}
	}

	private List<TeamRegistrationRow> buildTeamRegistrationRows(Long tournamentId, RegistrationStatus statusFilter, String search) {
		if (tournamentId == null) return List.of();

		List<TournamentRegistration> registrations = tournamentRegistrationService.listByTournamentIdWithTeam(tournamentId);
		List<TeamRegistrationRow> rows = new ArrayList<>();
		String query = (search == null) ? "" : search.toLowerCase().trim();

		for (TournamentRegistration registration : registrations) {
			if (registration == null) continue;
			if (statusFilter != null && registration.getStatus() != statusFilter) continue;
			if (registration.getTeam() == null || registration.getTeam().getId() == null) continue;

			var captain = registration.getTeam().getCaptain();
			String representative = captain == null || captain.getFullName() == null ? "Chưa cập nhật" : captain.getFullName();
			String phone = captain == null || captain.getPhone() == null ? "Chưa cập nhật" : captain.getPhone();
			String teamName = registration.getTeam().getName();

			if (!query.isEmpty()) {
				boolean matches = (teamName != null && teamName.toLowerCase().contains(query)) ||
						(representative != null && representative.toLowerCase().contains(query)) ||
						(phone != null && phone.toLowerCase().contains(query));
				if (!matches) continue;
			}

			long memberCount = playerRepository.countByTeamId(registration.getTeam().getId());
			rows.add(new TeamRegistrationRow(
					registration.getId(),
					viewFormatService.formatDate(registration.getCreatedAt()),
					teamName,
					representative,
					phone,
					memberCount,
					registration.getStatus()
			));
		}
		rows.sort(Comparator.comparing(TeamRegistrationRow::submittedDateText, Comparator.nullsLast(String::compareTo)).reversed());
		return rows;
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

	public record TeamManagementView(
			List<Tournament> tournaments,
			Long selectedTournamentId,
			String selectedStatus,
			List<TeamRegistrationRow> registrationRows,
			int currentPage,
			int totalPages,
			String currentSearch
	) {
	}

	public record UpdateStatusResult(boolean success, String message) {
	}

	public record TeamDetailView(
			Long registrationId,
			Long tournamentId,
			String submittedAt,
			String statusLabel,
			String statusClass,
			boolean canApproveOrReject,
			String teamName,
			String captainName,
			String captainPhone,
			String teamLogoUrl,
			String createdAt,
			long memberCount,
			List<Player> members
	) {
	}

	public record TeamRegistrationRow(
			Long registrationId,
			String submittedDateText,
			String teamName,
			String representativeName,
			String phone,
			long memberCount,
			RegistrationStatus status
	) {
		public String statusLabel() {
			if (status == null) return "Không xác định";
			return switch (status) {
				case PENDING -> "Chờ duyệt";
				case APPROVED -> "Đã duyệt";
				case REJECTED -> "Đã hủy";
			};
		}

		public String statusClass() {
			if (status == RegistrationStatus.APPROVED) return "badge--approved";
			if (status == RegistrationStatus.REJECTED) return "badge--rejected";
			return "badge--pending";
		}
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
