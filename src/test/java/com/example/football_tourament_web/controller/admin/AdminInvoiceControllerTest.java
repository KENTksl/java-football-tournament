package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminInvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminInvoiceControllerTest {
	@Test
	void invoiceManagement_rendersPage() throws Exception {
		AdminInvoiceService adminInvoiceService = mock(AdminInvoiceService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminInvoiceController(adminInvoiceService)).build();

		var viewModel = new AdminInvoiceService.InvoiceView(
				List.of(),
				"ALL",
				"time_desc",
				"",
				1,
				0,
				6
		);
		when(adminInvoiceService.queryInvoices(null, null, null, 1)).thenReturn(viewModel);

		mockMvc.perform(get("/admin/invoice-management"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/invoice/invoice-management"))
				.andExpect(model().attributeExists("transactions", "currentStatus", "currentSort", "currentSearch"));
	}
}
