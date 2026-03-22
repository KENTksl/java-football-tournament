package com.example.football_tourament_web.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MomoPaymentService {
	private final HttpClient httpClient;
	private static final Pattern PAY_URL_PATTERN = Pattern.compile("\"payUrl\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\"errorCode\"\\s*:\\s*(\\d+)");
	private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

	@Value("${momo.api-url}")
	private String apiUrl;

	@Value("${momo.partner-code}")
	private String partnerCode;

	@Value("${momo.access-key}")
	private String accessKey;

	@Value("${momo.secret-key}")
	private String secretKey;

	@Value("${momo.request-type}")
	private String requestType;

	@Value("${momo.return-url}")
	private String returnUrl;

	@Value("${momo.notify-url}")
	private String notifyUrl;

	public MomoPaymentService() {
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(10))
				.build();
	}

	public MomoCreatePaymentResponse createPayment(String orderId, String orderInfo, long amountVnd) throws Exception {
		String requestId = orderId;
		String rawData = "partnerCode=" + partnerCode
				+ "&accessKey=" + accessKey
				+ "&requestId=" + requestId
				+ "&amount=" + amountVnd
				+ "&orderId=" + orderId
				+ "&orderInfo=" + orderInfo
				+ "&returnUrl=" + returnUrl
				+ "&notifyUrl=" + notifyUrl
				+ "&extraData=";

		String signature = hmacSha256(rawData, secretKey);

		String json = "{"
				+ "\"accessKey\":\"" + escapeJson(accessKey) + "\","
				+ "\"partnerCode\":\"" + escapeJson(partnerCode) + "\","
				+ "\"requestType\":\"" + escapeJson(requestType) + "\","
				+ "\"notifyUrl\":\"" + escapeJson(notifyUrl) + "\","
				+ "\"returnUrl\":\"" + escapeJson(returnUrl) + "\","
				+ "\"orderId\":\"" + escapeJson(orderId) + "\","
				+ "\"amount\":\"" + amountVnd + "\","
				+ "\"orderInfo\":\"" + escapeJson(orderInfo) + "\","
				+ "\"requestId\":\"" + escapeJson(requestId) + "\","
				+ "\"extraData\":\"\","
				+ "\"signature\":\"" + escapeJson(signature) + "\""
				+ "}";

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/json; charset=UTF-8")
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (res.statusCode() < 200 || res.statusCode() >= 300) {
			MomoCreatePaymentResponse fail = new MomoCreatePaymentResponse();
			fail.errorCode = res.statusCode();
			fail.message = "HTTP " + res.statusCode();
			return fail;
		}

		if (res.body() == null || res.body().isBlank()) {
			MomoCreatePaymentResponse fail = new MomoCreatePaymentResponse();
			fail.errorCode = -1;
			fail.message = "Empty response";
			return fail;
		}

		MomoCreatePaymentResponse parsed = new MomoCreatePaymentResponse();
		parsed.raw = res.body();
		parsed.errorCode = extractInt(res.body(), ERROR_CODE_PATTERN);
		parsed.message = extractString(res.body(), MESSAGE_PATTERN);
		parsed.payUrl = extractString(res.body(), PAY_URL_PATTERN);
		return parsed;
	}

	private static String hmacSha256(String message, String key) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		mac.init(secretKeySpec);
		byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(digest);
	}

	public static class MomoCreatePaymentResponse {
		public Integer errorCode;
		public String message;
		public String payUrl;
		public String raw;
	}

	private static Integer extractInt(String input, Pattern pattern) {
		if (input == null) {
			return null;
		}
		Matcher m = pattern.matcher(input);
		if (!m.find()) {
			return null;
		}
		try {
			return Integer.parseInt(m.group(1));
		} catch (Exception ex) {
			return null;
		}
	}

	private static String extractString(String input, Pattern pattern) {
		if (input == null) {
			return null;
		}
		Matcher m = pattern.matcher(input);
		if (!m.find()) {
			return null;
		}
		return decodeJsonString(m.group(1));
	}

	private static String escapeJson(String input) {
		if (input == null) {
			return "";
		}
		return input
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
	}

	private static String decodeJsonString(String input) {
		if (input == null) {
			return null;
		}
		return input
				.replace("\\\"", "\"")
				.replace("\\\\", "\\")
				.replace("\\/", "/")
				.replace("\\n", " ")
				.replace("\\r", " ")
				.replace("\\t", " ");
	}
}
