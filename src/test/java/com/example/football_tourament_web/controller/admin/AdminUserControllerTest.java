package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.service.admin.AdminUserManagementService;
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

class AdminUserControllerTest {
	@Test
	void manageUser_rendersPage() throws Exception {
		AdminUserManagementService adminUserManagementService = mock(AdminUserManagementService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserManagementService)).build();

		when(adminUserManagementService.listUsers()).thenReturn(List.of());

		mockMvc.perform(get("/admin/manage/user"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/manage/manage-user"))
				.andExpect(model().attributeExists("users"));
	}

	@Test
	void userDetail_rendersPage_whenUserExists() throws Exception {
		AdminUserManagementService adminUserManagementService = mock(AdminUserManagementService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserManagementService)).build();

		AppUser user = new AppUser("Test User", "test@example.com");
		var viewModel = new AdminUserManagementService.UserDetailView(
				user,
				"01/01/2026",
				"User",
				"Đang kích hoạt",
				false,
				"Chưa có đội",
				false
		);
		when(adminUserManagementService.getUserDetail(1L)).thenReturn(viewModel);

		mockMvc.perform(get("/admin/manage/user-detail").param("userId", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/manage/user-detail"))
				.andExpect(model().attributeExists("user", "registeredAt", "roleLabel", "statusLabel"));
	}
}
