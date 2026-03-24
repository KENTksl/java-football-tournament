package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserPaymentControllerTest {
	@Test
	void momoNotify_returnsOk() throws Exception {
		UserPaymentService service = mock(UserPaymentService.class);
		when(service.handleMomoNotify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(ResponseEntity.ok("OK"));

		var controller = new UserPaymentController(service);
		var res = controller.momoNotify(java.util.Map.of("orderId", "TXN-ABC"), null, null, null, null);
		assertNotNull(res);
		assertEquals("OK", res.getBody());
	}
}
