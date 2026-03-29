package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserTeamService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserTeamControllerTest {
	@Test
	void teamInfo_redirectsWhenUnauthenticated() throws Exception {
		UserTeamService service = mock(UserTeamService.class);
		when(service.buildTeamInfoPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn("redirect:/dang-nhap");

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserTeamController(service)).build();

		mockMvc.perform(get("/thong-tin-doi"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dang-nhap"));
	}
}

