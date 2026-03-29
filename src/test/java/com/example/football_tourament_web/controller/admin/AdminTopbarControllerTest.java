package com.example.football_tourament_web.controller.admin;

import com.example.football_tourament_web.service.admin.AdminTopbarService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminTopbarControllerTest {
	@Test
	void messages_returnsJson() throws Exception {
		AdminTopbarService service = mock(AdminTopbarService.class);
		when(service.buildMessagesResponse()).thenReturn(new AdminTopbarService.AdminTopbarMessagesResponse(
				1,
				List.of(new AdminTopbarService.AdminTopbarMessage(1L, "A", "a@b.com", "hi", "now"))
		));

		var controller = new AdminTopbarController(service);
		var res = controller.topbarMessages();
		assertNotNull(res.getBody());
		assertEquals(1, res.getBody().getUnreadCount());
		assertEquals(1, res.getBody().getItems().size());
		assertEquals(1L, res.getBody().getItems().get(0).getId());
	}
}
