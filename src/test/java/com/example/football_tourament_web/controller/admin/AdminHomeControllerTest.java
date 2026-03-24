package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminHomeControllerTest {
	@Test
	void adminHome_rendersGeneralOverview() throws Exception {
		AdminDashboardService service = mock(AdminDashboardService.class);
		when(service.buildGeneralOverviewModel()).thenReturn(Map.of(
				"totalTournaments", 0L,
				"totalTeams", 0L,
				"activeTournaments", 0L,
				"completedTournaments", 0L,
				"recentWinners", java.util.List.of(),
				"matchFrequency", java.util.List.of()
		));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminHomeController(service)).build();

		mockMvc.perform(get("/admin"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/dashboard/general-overview"))
				.andExpect(model().attributeExists("totalTournaments", "totalTeams", "activeTournaments", "completedTournaments", "recentWinners", "matchFrequency"));
	}
}

