package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminInvoiceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminInvoiceController {
	private final AdminInvoiceService adminInvoiceService;

	public AdminInvoiceController(AdminInvoiceService adminInvoiceService) {
		this.adminInvoiceService = adminInvoiceService;
	}

	@GetMapping("/admin/invoice-management")
	public String invoiceManagement(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "sort", required = false) String sort,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "page", defaultValue = "1") int page,
			Model model
	) {
		var view = adminInvoiceService.queryInvoices(status, sort, search, page);
		model.addAttribute("transactions", view.getTransactions());
		model.addAttribute("currentStatus", view.getCurrentStatus());
		model.addAttribute("currentSort", view.getCurrentSort());
		model.addAttribute("currentSearch", view.getCurrentSearch());
		model.addAttribute("currentPage", view.getCurrentPage());
		model.addAttribute("totalPages", view.getTotalPages());
		model.addAttribute("pageSize", view.getPageSize());
		return "admin/invoice/invoice-management";
	}
}

