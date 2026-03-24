package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserHomeService;
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

class UserHomeControllerTest {
	@Test
	void home_renders() throws Exception {
		UserHomeService service = mock(UserHomeService.class);
		when(service.buildHomeView()).thenReturn(new UserHomeService.HomeView(List.of(), false));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserHomeController(service)).build();

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/home/index"))
				.andExpect(model().attributeExists("featuredTournaments", "hasFeaturedTournaments"));
	}
}

