package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminMatchHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMatchControllerTest {
	@Test
	void matchHistory_withoutTournamentId_redirects() throws Exception {
		AdminMatchHistoryService service = mock(AdminMatchHistoryService.class);
		when(service.buildMatchHistoryPage(null, null, null, 1, 10))
				.thenReturn(AdminMatchHistoryService.PageResult.redirect("/admin/manage/tournament"));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminMatchController(service)).build();

		mockMvc.perform(get("/admin/match-history"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/manage/tournament"));
	}

	@Test
	void matchHistory_withTournamentId_renders() throws Exception {
		AdminMatchHistoryService service = mock(AdminMatchHistoryService.class);
		when(service.buildMatchHistoryPage(1L, null, null, 1, 10))
				.thenReturn(AdminMatchHistoryService.PageResult.view("admin/tournament/match-history", Map.of("tournamentId", 1L)));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminMatchController(service)).build();

		mockMvc.perform(get("/admin/match-history").param("id", "1"))
				.andExpect(status().isOk());
	}
}

