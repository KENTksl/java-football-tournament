package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class UserPaymentController {
	private final UserPaymentService userPaymentService;

	public UserPaymentController(UserPaymentService userPaymentService) {
		this.userPaymentService = userPaymentService;
	}

	@GetMapping({"/thanh-toan", "/thanh-toan.html"})
	public String payment(Model model, Authentication authentication) {
		var user = userPaymentService.requireCurrentUser(authentication);
		if (user == null) return "redirect:/dang-nhap";

		userPaymentService.attachCommonProfileModel(model, user);
		model.addAttribute("paymentHasResult", false);
		model.addAttribute("paymentSuccess", false);
		if (!model.containsAttribute("paymentForm")) {
			model.addAttribute("paymentForm", new UserPaymentService.PaymentForm());
		}
		return "user/profile/payment";
	}

	@GetMapping("/thanh-toan/ket-qua")
	public String paymentResult(@RequestParam(name = "code", required = false) String code, Model model, Authentication authentication) {
		var user = userPaymentService.requireCurrentUser(authentication);
		if (user == null) return "redirect:/dang-nhap";
		if (code == null || code.isBlank()) return "redirect:/thanh-toan";

		var result = userPaymentService.buildPaymentResultView(code, user);
		if (result == null) return "redirect:/thanh-toan";

		userPaymentService.attachCommonProfileModel(model, user);
		model.addAttribute("paymentHasResult", true);
		model.addAttribute("paymentSuccess", result.paymentSuccess());
		model.addAttribute("paymentResultAmount", result.paymentResultAmount());
		model.addAttribute("paymentResultStatusLabel", result.paymentResultStatusLabel());
		model.addAttribute("paymentResultStatusClass", result.paymentResultStatusClass());
		model.addAttribute("paymentResultIconClass", result.paymentResultIconClass());
		model.addAttribute("paymentResultIconChar", result.paymentResultIconChar());
		model.addAttribute("paymentResultTitle", result.paymentResultTitle());
		return "user/profile/payment";
	}

	@PostMapping("/thanh-toan")
	public String paymentSubmit(
			@Valid UserPaymentService.PaymentForm paymentForm,
			BindingResult bindingResult,
			Authentication authentication,
			Model model
	) {
		var user = userPaymentService.requireCurrentUser(authentication);
		return userPaymentService.handlePaymentSubmit(paymentForm, bindingResult, user, model);
	}

	@GetMapping({"/thanh-toan/momo/callback", "/order/momo-return"})
	public String momoCallback(
			@RequestParam(name = "orderId", required = false) String orderId,
			@RequestParam(name = "requestId", required = false) String requestId,
			@RequestParam(name = "errorCode", required = false) String errorCode,
			@RequestParam(name = "resultCode", required = false) String resultCode
	) {
		return userPaymentService.handleMomoCallback(orderId, requestId, errorCode, resultCode);
	}

	@PostMapping({"/thanh-toan/momo/notify", "/order/momo-notify"})
	@ResponseBody
	public ResponseEntity<String> momoNotify(
			@RequestBody(required = false) Map<String, Object> body,
			@RequestParam(name = "orderId", required = false) String orderIdParam,
			@RequestParam(name = "requestId", required = false) String requestIdParam,
			@RequestParam(name = "errorCode", required = false) String errorCodeParam,
			@RequestParam(name = "resultCode", required = false) String resultCodeParam
	) {
		return userPaymentService.handleMomoNotify(body, orderIdParam, requestIdParam, errorCodeParam, resultCodeParam);
	}
}

