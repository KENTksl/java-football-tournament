package com.example.football_tourament_web.service.user;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.service.user.MomoPaymentService;
import com.example.football_tourament_web.service.core.TransactionService;
import com.example.football_tourament_web.service.common.ViewFormatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UserPaymentService {
	private final TransactionService transactionService;
	private final MomoPaymentService momoPaymentService;
	private final UserProfileService userProfileService;
	private final ViewFormatService viewFormatService;

	public UserPaymentService(
			TransactionService transactionService,
			MomoPaymentService momoPaymentService,
			UserProfileService userProfileService,
			ViewFormatService viewFormatService
	) {
		this.transactionService = transactionService;
		this.momoPaymentService = momoPaymentService;
		this.userProfileService = userProfileService;
		this.viewFormatService = viewFormatService;
	}

	public AppUser requireCurrentUser(Authentication authentication) {
		return userProfileService.requireCurrentUser(authentication);
	}

	public void attachCommonProfileModel(Model model, AppUser user) {
		userProfileService.attachCommonProfileModel(model, user);
	}

	public PaymentResultView buildPaymentResultView(String code, AppUser user) {
		if (code == null || code.isBlank() || user == null) return null;

		Transaction tx = transactionService.findByCode(code.trim()).orElse(null);
		if (tx == null || tx.getUser() == null || !user.getId().equals(tx.getUser().getId())) {
			return null;
		}

		return new PaymentResultView(
				tx.getStatus() == TransactionStatus.SUCCESS,
				viewFormatService.formatMoney(tx.getAmount()),
				transactionStatusLabel(tx.getStatus()),
				transactionStatusClass(tx.getStatus()),
				paymentResultIconClass(tx.getStatus()),
				paymentResultIconChar(tx.getStatus()),
				paymentResultTitle(tx.getStatus())
		);
	}

	public String handlePaymentSubmit(@Valid PaymentForm paymentForm, BindingResult bindingResult, AppUser user, Model model) {
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		Integer option = paymentForm.amountOption;
		List<Integer> allowed = List.of(10, 20, 50, 100, 200, 500);
		if (option == null || !allowed.contains(option)) {
			bindingResult.rejectValue("amountOption", "amountOption.invalid", "Vui lòng chọn số tiền 10k/20k/50k/100k/200k/500k");
		}

		if (bindingResult.hasErrors()) {
			attachCommonProfileModel(model, user);
			model.addAttribute("paymentHasResult", false);
			model.addAttribute("paymentSuccess", false);
			model.addAttribute("paymentForm", paymentForm);
			return "user/profile/payment";
		}

		String code = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		BigDecimal amount = BigDecimal.valueOf(option.longValue()).multiply(BigDecimal.valueOf(1000));
		String desc = "Nạp tiền MoMo";

		Transaction tx = new Transaction(code, desc, amount, user);
		tx.setStatus(TransactionStatus.PENDING);
		transactionService.save(tx);

		try {
			String orderInfo = desc + " - " + (user.getFullName() == null ? user.getEmail() : user.getFullName());
			var momoRes = momoPaymentService.createPayment(code, orderInfo, amount.longValue());
			if (momoRes != null && momoRes.errorCode != null && momoRes.errorCode == 0 && momoRes.payUrl != null && !momoRes.payUrl.isBlank()) {
				return "redirect:" + momoRes.payUrl;
			}
			tx.setStatus(TransactionStatus.FAILED);
			transactionService.save(tx);
			return "redirect:/thanh-toan/ket-qua?code=" + code;
		} catch (Exception ex) {
			tx.setStatus(TransactionStatus.FAILED);
			transactionService.save(tx);
			return "redirect:/thanh-toan/ket-qua?code=" + code;
		}
	}

	public String handleMomoCallback(String orderId, String requestId, String errorCode, String resultCode) {
		String resolvedOrderId = orderId;
		if ((resolvedOrderId == null || resolvedOrderId.isBlank()) && requestId != null && !requestId.isBlank()) {
			resolvedOrderId = requestId;
		}

		if (resolvedOrderId == null || resolvedOrderId.isBlank()) {
			return "redirect:/thanh-toan";
		}

		Transaction tx = transactionService.findByCode(resolvedOrderId.trim()).orElse(null);
		if (tx != null) {
			Integer ec = parseIntOrNull(errorCode);
			Integer rc = parseIntOrNull(resultCode);
			boolean ok = (ec != null && ec == 0) || (rc != null && rc == 0);
			tx.setStatus(ok ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
			transactionService.save(tx);
		}

		return "redirect:/thanh-toan/ket-qua?code=" + resolvedOrderId.trim();
	}

	public ResponseEntity<String> handleMomoNotify(
			Map<String, Object> body,
			String orderIdParam,
			String requestIdParam,
			String errorCodeParam,
			String resultCodeParam
	) {
		String orderId = orderIdParam;
		String requestId = requestIdParam;
		String errorCode = errorCodeParam;
		String resultCode = resultCodeParam;

		if (body != null) {
			Object oid = body.get("orderId");
			Object rid = body.get("requestId");
			Object ec = body.get("errorCode");
			Object rc = body.get("resultCode");
			if (orderId == null && oid != null) {
				orderId = oid.toString();
			}
			if (requestId == null && rid != null) {
				requestId = rid.toString();
			}
			if (errorCode == null && ec != null) {
				errorCode = ec.toString();
			}
			if (resultCode == null && rc != null) {
				resultCode = rc.toString();
			}
		}

		String resolvedOrderId = orderId;
		if ((resolvedOrderId == null || resolvedOrderId.isBlank()) && requestId != null && !requestId.isBlank()) {
			resolvedOrderId = requestId;
		}

		if (resolvedOrderId != null && !resolvedOrderId.isBlank()) {
			Transaction tx = transactionService.findByCode(resolvedOrderId.trim()).orElse(null);
			if (tx != null) {
				Integer ec = parseIntOrNull(errorCode);
				Integer rc = parseIntOrNull(resultCode);
				boolean ok = (ec != null && ec == 0) || (rc != null && rc == 0);
				tx.setStatus(ok ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
				transactionService.save(tx);
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body("OK");
	}

	private static Integer parseIntOrNull(String value) {
		if (value == null) {
			return null;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception ex) {
			return null;
		}
	}

	private static String transactionStatusLabel(TransactionStatus status) {
		if (status == null) return "";
		return switch (status) {
			case PENDING -> "Đang chờ thanh toán";
			case SUCCESS -> "Thành công";
			case FAILED -> "Thất bại";
		};
	}

	private static String transactionStatusClass(TransactionStatus status) {
		if (status == null) return "badge--muted";
		return switch (status) {
			case PENDING -> "badge--pending";
			case SUCCESS -> "badge--success";
			case FAILED -> "badge--failed";
		};
	}

	private static String paymentResultTitle(TransactionStatus status) {
		if (status == TransactionStatus.SUCCESS) return "Thanh toán thành công";
		if (status == TransactionStatus.FAILED) return "Thanh toán thất bại";
		return "Đang xử lý thanh toán";
	}

	private static String paymentResultIconClass(TransactionStatus status) {
		if (status == TransactionStatus.SUCCESS) return "payment-result__icon payment-result__icon--success";
		if (status == TransactionStatus.FAILED) return "payment-result__icon payment-result__icon--failed";
		return "payment-result__icon payment-result__icon--pending";
	}

	private static String paymentResultIconChar(TransactionStatus status) {
		if (status == TransactionStatus.SUCCESS) return "✓";
		if (status == TransactionStatus.FAILED) return "×";
		return "…";
	}

	public static class PaymentForm {
		private Integer amountOption;

		public Integer getAmountOption() { return amountOption; }
		public void setAmountOption(Integer amountOption) { this.amountOption = amountOption; }
	}

	public record PaymentResultView(
			boolean paymentSuccess,
			String paymentResultAmount,
			String paymentResultStatusLabel,
			String paymentResultStatusClass,
			String paymentResultIconClass,
			String paymentResultIconChar,
			String paymentResultTitle
	) {
	}
}
