package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {
	@Test
	void profile_withoutAuth_redirectsToLogin() throws Exception {
		UserProfileService service = mock(UserProfileService.class);
		when(service.requireCurrentUser(org.mockito.ArgumentMatchers.any())).thenReturn(null);

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserProfileController(service)).build();

		mockMvc.perform(get("/ca-nhan"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dang-nhap"));
	}
}

