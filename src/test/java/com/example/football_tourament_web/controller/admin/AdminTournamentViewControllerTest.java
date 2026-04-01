package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTournamentViewService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTournamentViewControllerTest {
	@Test
	void generalInformation_withoutId_redirects() throws Exception {
		AdminTournamentViewService service = mock(AdminTournamentViewService.class);
		when(service.buildGeneralInformationPage(null)).thenReturn(AdminTournamentViewService.PageResult.redirect("/admin/manage/tournament"));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminTournamentController(service)).build();

		mockMvc.perform(get("/admin/general-information"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/manage/tournament"));
	}

	@Test
	void tournamentBracket_renders() throws Exception {
		AdminTournamentViewService service = mock(AdminTournamentViewService.class);
		when(service.buildTournamentBracketPage(1L)).thenReturn(AdminTournamentViewService.PageResult.view("admin/tournament/tournament-bracket", Map.of("tournamentId", 1L)));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminTournamentController(service)).build();

		mockMvc.perform(get("/admin/tournament-bracket").param("id", "1"))
				.andExpect(status().isOk());
	}
}

