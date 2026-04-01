package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class UserAuthControllerTest {
	@Test
	void registerPage_renders() throws Exception {
		UserAuthService service = mock(UserAuthService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserAuthController(service)).build();

		mockMvc.perform(get("/dang-ky"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/auth/register"));
	}

	@Test
	void registerSubmit_delegatesToService() throws Exception {
		UserAuthService service = mock(UserAuthService.class);
		when(service.handleRegister(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn("redirect:/dang-nhap?registered");
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserAuthController(service)).build();

		mockMvc.perform(post("/dang-ky")
						.param("fullName", "U")
						.param("email", "u@u.com")
						.param("phone", "0123")
						.param("password", "secret12")
						.param("confirmPassword", "secret12")
						.param("acceptTerms", "true"))
				.andExpect(status().is3xxRedirection());
	}
}

