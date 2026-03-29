package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.entity.TournamentRegistration;
import com.example.football_tourament_web.service.core.ContactMessageService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TransactionService;
import com.example.football_tourament_web.service.common.ViewFormatService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminTopbarService {
	private final ContactMessageService contactMessageService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final TransactionService transactionService;
	private final ViewFormatService viewFormatService;

	public AdminTopbarService(
			ContactMessageService contactMessageService,
			TournamentRegistrationService tournamentRegistrationService,
			TransactionService transactionService,
			ViewFormatService viewFormatService
	) {
		this.contactMessageService = contactMessageService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.transactionService = transactionService;
		this.viewFormatService = viewFormatService;
	}

	public TopbarModel buildModel() {
		List<AdminTopbarMessage> messages;
		long unreadCount;
		try {
			messages = contactMessageService.listRecent(5).stream()
					.map(m -> new AdminTopbarMessage(
							m.getId(),
							m.getName(),
							m.getEmail(),
							m.getMessage(),
							viewFormatService.formatDateTime(m.getCreatedAt())
					))
					.toList();
			unreadCount = contactMessageService.countUnread();
		} catch (Exception ex) {
			messages = List.of();
			unreadCount = 0;
		}

		List<AdminTopbarNotification> notifications = new ArrayList<>();
		long pendingCount;
		try {
			for (TournamentRegistration r : tournamentRegistrationService.listRecentPendingWithDetails(5)) {
				String teamName = r.getTeam() == null ? "Đội" : (r.getTeam().getName() == null ? "Đội" : r.getTeam().getName());
				String tournamentName = r.getTournament() == null ? "giải đấu" : (r.getTournament().getName() == null ? "giải đấu" : r.getTournament().getName());
				String title = "Đội đăng ký giải";
				String detail = teamName + " • " + tournamentName;
				String href = "/admin/team-detail?id=" + r.getId();
				notifications.add(new AdminTopbarNotification(title, detail, viewFormatService.formatDateTime(r.getCreatedAt()), href));
			}
			pendingCount = tournamentRegistrationService.countPending();
		} catch (Exception ex) {
			pendingCount = 0;
		}

		try {
			List<Transaction> txs = transactionService.listAll().stream().limit(5).toList();
			for (Transaction t : txs) {
				String userName = t.getUser() == null ? "Người dùng" : (t.getUser().getFullName() == null ? "Người dùng" : t.getUser().getFullName());
				String title = "Giao dịch mới";
				String detail = userName + " • " + (viewFormatService.formatMoney(t.getAmount()) == null ? "" : viewFormatService.formatMoney(t.getAmount()));
				String href = "/admin/invoice-management?status=ALL&search=" + (t.getCode() == null ? "" : t.getCode());
				notifications.add(new AdminTopbarNotification(title, detail, viewFormatService.formatDateTime(t.getCreatedAt()), href));
			}
		} catch (Exception ex) {
			// ignore
		}

		return new TopbarModel(messages, unreadCount, notifications, pendingCount);
	}

	public AdminTopbarMessagesResponse buildMessagesResponse() {
		try {
			List<AdminTopbarMessage> messages = contactMessageService.listRecent(5).stream()
					.map(m -> new AdminTopbarMessage(
							m.getId(),
							m.getName(),
							m.getEmail(),
							m.getMessage(),
							viewFormatService.formatDateTime(m.getCreatedAt())
					))
					.toList();
			long unread = contactMessageService.countUnread();
			return new AdminTopbarMessagesResponse(unread, messages);
		} catch (Exception ex) {
			return new AdminTopbarMessagesResponse(0, List.of());
		}
	}

	public AdminTopbarNotificationsResponse buildNotificationsResponse() {
		try {
			List<AdminTopbarNotification> notifications = new ArrayList<>();
			for (TournamentRegistration r : tournamentRegistrationService.listRecentPendingWithDetails(5)) {
				String teamName = r.getTeam() == null ? "Đội" : (r.getTeam().getName() == null ? "Đội" : r.getTeam().getName());
				String tournamentName = r.getTournament() == null ? "giải đấu" : (r.getTournament().getName() == null ? "giải đấu" : r.getTournament().getName());
				String title = "Đội đăng ký giải";
				String detail = teamName + " • " + tournamentName;
				String href = "/admin/team-detail?id=" + r.getId();
				notifications.add(new AdminTopbarNotification(title, detail, viewFormatService.formatDateTime(r.getCreatedAt()), href));
			}

			List<Transaction> txs = transactionService.listAll().stream().limit(5).toList();
			for (Transaction t : txs) {
				String userName = t.getUser() == null ? "Người dùng" : (t.getUser().getFullName() == null ? "Người dùng" : t.getUser().getFullName());
				String title = "Giao dịch mới";
				String detail = userName + " • " + (viewFormatService.formatMoney(t.getAmount()) == null ? "" : viewFormatService.formatMoney(t.getAmount()));
				String href = "/admin/invoice-management?status=ALL&search=" + (t.getCode() == null ? "" : t.getCode());
				notifications.add(new AdminTopbarNotification(title, detail, viewFormatService.formatDateTime(t.getCreatedAt()), href));
			}

			long pending = tournamentRegistrationService.countPending();
			return new AdminTopbarNotificationsResponse(pending, notifications);
		} catch (Exception ex) {
			return new AdminTopbarNotificationsResponse(0, List.of());
		}
	}

	public record TopbarModel(
			List<AdminTopbarMessage> messages,
			long unreadMessageCount,
			List<AdminTopbarNotification> notifications,
			long pendingRegistrationCount
	) {
	}

	public static final class AdminTopbarMessage {
		private final Long id;
		private final String name;
		private final String email;
		private final String message;
		private final String timeText;

		public AdminTopbarMessage(Long id, String name, String email, String message, String timeText) {
			this.id = id;
			this.name = name;
			this.email = email;
			this.message = message;
			this.timeText = timeText;
		}

		public Long getId() { return id; }
		public String getName() { return name; }
		public String getEmail() { return email; }
		public String getMessage() { return message; }
		public String getTimeText() { return timeText; }
	}

	public static final class AdminTopbarNotification {
		private final String title;
		private final String detail;
		private final String timeText;
		private final String href;

		public AdminTopbarNotification(String title, String detail, String timeText, String href) {
			this.title = title;
			this.detail = detail;
			this.timeText = timeText;
			this.href = href;
		}

		public String getTitle() { return title; }
		public String getDetail() { return detail; }
		public String getTimeText() { return timeText; }
		public String getHref() { return href; }
	}

	public static final class AdminTopbarMessagesResponse {
		private final long unreadCount;
		private final List<AdminTopbarMessage> items;

		public AdminTopbarMessagesResponse(long unreadCount, List<AdminTopbarMessage> items) {
			this.unreadCount = unreadCount;
			this.items = items;
		}

		public long getUnreadCount() { return unreadCount; }
		public List<AdminTopbarMessage> getItems() { return items; }
	}

	public static final class AdminTopbarNotificationsResponse {
		private final long pendingCount;
		private final List<AdminTopbarNotification> items;

		public AdminTopbarNotificationsResponse(long pendingCount, List<AdminTopbarNotification> items) {
			this.pendingCount = pendingCount;
			this.items = items;
		}

		public long getPendingCount() { return pendingCount; }
		public List<AdminTopbarNotification> getItems() { return items; }
	}
}

