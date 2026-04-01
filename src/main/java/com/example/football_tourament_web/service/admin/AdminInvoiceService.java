package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.service.core.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminInvoiceService {
	private final TransactionService transactionService;

	public AdminInvoiceService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@Transactional(readOnly = true)
	public InvoiceView queryInvoices(String status, String sort, String search, int page) {
		List<Transaction> transactions = transactionService.listAll();

		String normalizedStatus = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
		if (!normalizedStatus.isEmpty() && !"ALL".equals(normalizedStatus)) {
			try {
				TransactionStatus targetStatus = TransactionStatus.valueOf(normalizedStatus);
				transactions = transactions.stream()
						.filter(t -> t != null && t.getStatus() == targetStatus)
						.toList();
			} catch (IllegalArgumentException ignored) {
			}
		}

		if (search != null && !search.trim().isEmpty()) {
			String query = search.toLowerCase(Locale.ROOT).trim();
			transactions = transactions.stream()
					.filter(t -> {
						if (t == null) return false;
						boolean codeMatch = t.getCode() != null && t.getCode().toLowerCase(Locale.ROOT).contains(query);
						boolean userMatch = t.getUser() != null
								&& t.getUser().getFullName() != null
								&& t.getUser().getFullName().toLowerCase(Locale.ROOT).contains(query);
						return codeMatch || userMatch;
					})
					.toList();
		}

		String normalizedSort = sort == null ? "time_desc" : sort.trim().toLowerCase(Locale.ROOT);
		if ("time_asc".equals(normalizedSort)) {
			transactions = transactions.stream().sorted(Comparator.comparing(Transaction::getCreatedAt)).toList();
		} else if ("time_desc".equals(normalizedSort)) {
			transactions = transactions.stream().sorted(Comparator.comparing(Transaction::getCreatedAt).reversed()).toList();
		} else if ("value_asc".equals(normalizedSort)) {
			transactions = transactions.stream().sorted(Comparator.comparing(Transaction::getAmount)).toList();
		} else if ("value_desc".equals(normalizedSort)) {
			transactions = transactions.stream().sorted(Comparator.comparing(Transaction::getAmount).reversed()).toList();
		}

		int pageSize = 6;
		int totalItems = transactions.size();
		int totalPages = (int) Math.ceil((double) totalItems / pageSize);

		int currentPage = page;
		if (currentPage < 1) currentPage = 1;
		if (totalPages > 0 && currentPage > totalPages) currentPage = totalPages;

		int start = (currentPage - 1) * pageSize;
		int end = Math.min(start + pageSize, totalItems);
		List<Transaction> paged = (totalItems > 0 && start < totalItems)
				? transactions.subList(start, end)
				: Collections.emptyList();

		return new InvoiceView(
				paged,
				normalizedStatus.isEmpty() ? "ALL" : normalizedStatus,
				normalizedSort,
				search == null ? "" : search,
				currentPage,
				totalPages,
				pageSize
		);
	}

	public static final class InvoiceView {
		private final List<Transaction> transactions;
		private final String currentStatus;
		private final String currentSort;
		private final String currentSearch;
		private final int currentPage;
		private final int totalPages;
		private final int pageSize;

		public InvoiceView(List<Transaction> transactions, String currentStatus, String currentSort, String currentSearch, int currentPage, int totalPages, int pageSize) {
			this.transactions = transactions;
			this.currentStatus = currentStatus;
			this.currentSort = currentSort;
			this.currentSearch = currentSearch;
			this.currentPage = currentPage;
			this.totalPages = totalPages;
			this.pageSize = pageSize;
		}

		public List<Transaction> getTransactions() { return transactions; }
		public String getCurrentStatus() { return currentStatus; }
		public String getCurrentSort() { return currentSort; }
		public String getCurrentSearch() { return currentSearch; }
		public int getCurrentPage() { return currentPage; }
		public int getTotalPages() { return totalPages; }
		public int getPageSize() { return pageSize; }
	}
}
