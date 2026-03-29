package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.service.admin.AdminProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminProfileControllerTest {
	@Test
	void profile_rendersPage() throws Exception {
		AdminProfileService service = mock(AdminProfileService.class);
		when(service.findAdmin(org.mockito.ArgumentMatchers.any())).thenReturn(new AppUser());
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminProfileController(service)).build();

		mockMvc.perform(get("/admin/profile").principal(() -> "admin@example.com"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/profile/admin-profile"))
				.andExpect(model().attributeExists("admin"));
	}

	@Test
	void saveProfile_redirects() throws Exception {
		AdminProfileService service = mock(AdminProfileService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminProfileController(service)).build();

		mockMvc.perform(post("/admin/profile/save")
						.principal(() -> "admin@example.com")
						.param("fullName", "Admin")
						.param("phone", "0123")
						.param("address", "HN"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/profile"));
	}
}

