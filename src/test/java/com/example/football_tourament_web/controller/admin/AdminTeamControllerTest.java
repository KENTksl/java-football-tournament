package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTeamManagementService;
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

class AdminTeamControllerTest {
	@Test
	void teamManagement_rendersPage() throws Exception {
		AdminTeamManagementService service = mock(AdminTeamManagementService.class);
		when(service.buildTeamManagementView(null, "ALL", null, 1, 10)).thenReturn(
				new AdminTeamManagementService.TeamManagementView(
						List.of(),
						1L,
						"ALL",
						List.of(),
						1,
						1,
						null
				)
		);

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminTeamController(service)).build();

		mockMvc.perform(get("/admin/team-management"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/team/team-management"))
				.andExpect(model().attributeExists("tournaments", "selectedTournamentId", "selectedStatus", "registrationRows", "currentPage", "totalPages"));
	}
}
