package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.core.TournamentService;
import com.example.football_tourament_web.service.user.UserTournamentViewService;
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

class UserTournamentControllerTest {
	@Test
	void tournamentList_renders() throws Exception {
		TournamentService tournamentService = mock(TournamentService.class);
		UserTournamentViewService viewService = mock(UserTournamentViewService.class);

		when(tournamentService.listTournamentsNewestFirst()).thenReturn(List.of());
		when(viewService.buildRegisteredTeamCountMap(List.of())).thenReturn(Map.of());
		when(viewService.buildRegisteredTeamPercentMap(List.of())).thenReturn(Map.of());

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserTournamentController(tournamentService, viewService)).build();

		mockMvc.perform(get("/user/tournament"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/tournament/tournament-list"))
				.andExpect(model().attributeExists("tournaments", "registeredTeamCounts", "registeredTeamPercents"));
	}
}
