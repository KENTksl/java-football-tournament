package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTournamentManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminTournamentControllerTest {
	@Test
	void manageTournament_rendersPage() throws Exception {
		AdminTournamentManagementService adminTournamentManagementService = mock(AdminTournamentManagementService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminTournamentManagementController(adminTournamentManagementService)).build();

		when(adminTournamentManagementService.listTournaments()).thenReturn(List.of());
		when(adminTournamentManagementService.buildRegisteredTeamCountMap(List.of())).thenReturn(Map.of());
		when(adminTournamentManagementService.buildTournamentRows(List.of(), Map.of())).thenReturn(List.of());

		mockMvc.perform(get("/admin/manage/tournament"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/manage/manage-tournament"))
				.andExpect(model().attributeExists("tournaments", "registeredTeamCounts", "tournamentRows"));
	}
}
