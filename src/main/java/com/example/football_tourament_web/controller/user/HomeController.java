package com.example.football_tourament_web.controller.user;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.football_tourament_web.model.dto.NewsItem;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.Gender;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.CbsSportsNewsService;
import com.example.football_tourament_web.service.TeamService;
import com.example.football_tourament_web.service.TournamentRegistrationService;
import com.example.football_tourament_web.service.TransactionService;
import com.example.football_tourament_web.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class HomeController {
	private final CbsSportsNewsService cbsSportsNewsService;
	private final UserService userService;
	private final TransactionService transactionService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final TeamService teamService;
	private final PlayerRepository playerRepository;
	private final HttpClient httpClient;
	private static final Pattern LAT_PATTERN = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern LON_PATTERN = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"");

	public HomeController(
			CbsSportsNewsService cbsSportsNewsService,
			UserService userService,
			TransactionService transactionService,
			TournamentRegistrationService tournamentRegistrationService,
			TeamService teamService,
			PlayerRepository playerRepository
	) {
		this.cbsSportsNewsService = cbsSportsNewsService;
		this.userService = userService;
		this.transactionService = transactionService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.teamService = teamService;
		this.playerRepository = playerRepository;
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(10))
				.build();
	}

	@GetMapping({"/", "/home"})
	public String home() {
		return "user/home/index";
	}

	@GetMapping({"/tin-tuc", "/tin-tuc.html"})
	public String news(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
		int pageSize = 6;
		List<NewsItem> allItems = cbsSportsNewsService.getHeadlines();

		int totalItems = allItems.size();
		int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
		int currentPage = Math.min(Math.max(page, 1), totalPages);

		int fromIndex = Math.min((currentPage - 1) * pageSize, totalItems);
		int toIndex = Math.min(fromIndex + pageSize, totalItems);
		List<NewsItem> pageItems = allItems.subList(fromIndex, toIndex);

		int startPage = Math.max(1, currentPage - 2);
		int endPage = Math.min(totalPages, currentPage + 2);
		if (endPage - startPage + 1 < 5) {
			endPage = Math.min(totalPages, startPage + 4);
			startPage = Math.max(1, endPage - 4);
		}

		model.addAttribute("newsItems", pageItems);
		model.addAttribute("currentPage", currentPage);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);
		model.addAttribute("hasNews", !allItems.isEmpty());
		return "user/home/news";
	}

	@GetMapping({"/gioi-thieu", "/gioi-thieu.html"})
	public String aboutUs() {
		return "user/home/about-us";
	}

	@GetMapping({"/lien-he", "/lien-he.html"})
	public String contact() {
		return "user/home/contact";
	}

	@GetMapping("/api/geocode")
	@ResponseBody
	public ResponseEntity<GeocodeResult> geocode(@RequestParam(name = "q") String query) {
		if (query == null || query.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
					+ java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);

			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(10))
					.header("Accept", "application/json")
					.header("User-Agent", "football-tourament-web/1.0 (contact page map)")
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
			}

			String body = response.body();

			Matcher latMatcher = LAT_PATTERN.matcher(body);
			Matcher lonMatcher = LON_PATTERN.matcher(body);
			if (!latMatcher.find() || !lonMatcher.find()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}

			double lat = Double.parseDouble(latMatcher.group(1));
			double lon = Double.parseDouble(lonMatcher.group(1));

			Matcher displayNameMatcher = DISPLAY_NAME_PATTERN.matcher(body);
			String displayName = displayNameMatcher.find() ? decodeJsonString(displayNameMatcher.group(1)) : null;

			return ResponseEntity.ok(new GeocodeResult(lat, lon, displayName));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
		}
	}

	@GetMapping({"/dang-nhap", "/dang-nhap.html"})
	public String login() {
		return "user/auth/login";
	}

	@GetMapping({"/dang-ky", "/dang-ky.html"})
	public String register(Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new RegisterForm());
		}
		return "user/auth/register";
	}

	@PostMapping("/dang-ky")
	public String registerSubmit(@Valid RegisterForm form, BindingResult bindingResult, Model model) {
		if (!form.password.equals(form.confirmPassword)) {
			bindingResult.rejectValue("confirmPassword", "confirmPassword.mismatch", "Mật khẩu nhập lại không khớp");
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("form", form);
			return "user/auth/register";
		}

		try {
			userService.registerUser(form.fullName, form.email, form.phone, form.password);
			return "redirect:/dang-nhap?registered";
		} catch (IllegalArgumentException ex) {
			String msg = ex.getMessage() == null ? "Đăng ký thất bại" : ex.getMessage();
			String lower = msg.toLowerCase();
			if (lower.contains("email")) {
				bindingResult.rejectValue("email", "email.exists", msg);
			} else if (lower.contains("điện thoại") || lower.contains("phone")) {
				bindingResult.rejectValue("phone", "phone.exists", msg);
			} else {
				bindingResult.reject("register.failed", msg);
			}
			model.addAttribute("form", form);
			return "user/auth/register";
		} catch (Exception ex) {
			bindingResult.reject("register.failed", "Đăng ký thất bại. Vui lòng kiểm tra lại thông tin và thử lại.");
			model.addAttribute("form", form);
			return "user/auth/register";
		}
	}

	@GetMapping({"/ca-nhan", "/ca-nhan.html"})
	public String profile(Model model, Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		attachCommonProfileModel(model, user);
		if (!model.containsAttribute("profileForm")) {
			model.addAttribute("profileForm", ProfileForm.fromUser(user));
		}
		return "user/profile/profile";
	}

	@PostMapping("/ca-nhan")
	public String updateProfile(
			@Valid ProfileForm profileForm,
			BindingResult bindingResult,
			@RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
			Authentication authentication,
			Model model
	) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		if (bindingResult.hasErrors()) {
			attachCommonProfileModel(model, user);
			model.addAttribute("profileForm", profileForm);
			return "user/profile/profile";
		}

		try {
			boolean hasAvatarUrlInput = profileForm.avatarUrl != null && !profileForm.avatarUrl.isBlank();
			boolean hasAvatarFile = avatarFile != null && !avatarFile.isEmpty();

			String nextAvatarUrl;
			if (hasAvatarUrlInput) {
				nextAvatarUrl = profileForm.avatarUrl.trim();
			} else if (hasAvatarFile) {
				String stored = storeAvatarFile(avatarFile);
				if (stored == null) {
					return "redirect:/ca-nhan?avatarError";
				}
				nextAvatarUrl = stored;
			} else {
				nextAvatarUrl = user.getAvatarUrl();
				if (nextAvatarUrl == null || nextAvatarUrl.isBlank()) {
					nextAvatarUrl = user.getAvatar();
				}
			}

			userService.updateProfile(
					user.getEmail(),
					profileForm.fullName,
					profileForm.phone,
					profileForm.address,
					profileForm.gender,
					profileForm.dateOfBirth,
					nextAvatarUrl
			);
			if (hasAvatarFile && !hasAvatarUrlInput) {
				return "redirect:/ca-nhan?updatedAvatar";
			}
			return "redirect:/ca-nhan?updated";
		} catch (IllegalArgumentException ex) {
			bindingResult.reject("profile.update", ex.getMessage());
			attachCommonProfileModel(model, user);
			model.addAttribute("profileForm", profileForm);
			return "user/profile/profile";
		} catch (AvatarTooLargeException ex) {
			return "redirect:/ca-nhan?avatarTooLarge";
		} catch (AvatarInvalidTypeException ex) {
			return "redirect:/ca-nhan?avatarInvalidType";
		} catch (Exception ex) {
			return "redirect:/ca-nhan?avatarError";
		}
	}

	@GetMapping({"/thong-tin-doi", "/thong-tin-doi.html"})
	public String teamInfo(Model model, Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		attachCommonProfileModel(model, user);

		var teamOpt = teamService.findCaptainTeam(user.getId());
		if (teamOpt.isEmpty()) {
			model.addAttribute("teamName", "");
			model.addAttribute("captainName", user.getFullName());
			model.addAttribute("memberCount", 0);
			model.addAttribute("members", List.of());
			return "user/profile/team-info";
		}

		var team = teamOpt.get();
		var players = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId());
		model.addAttribute("teamName", team.getName());
		model.addAttribute("captainName", user.getFullName());
		model.addAttribute("memberCount", players.size());
		model.addAttribute("members", players);
		return "user/profile/team-info";
	}

	@GetMapping({"/lich-su-dang-ky", "/lich-su-dang-ky.html"})
	public String registrationHistory(Model model, Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		attachCommonProfileModel(model, user);
		var registrations = tournamentRegistrationService.listByUserId(user.getId()).stream()
				.map(r -> new RegistrationView(
						r.getTournament().getName(),
						r.getTeam().getName(),
						formatInstant(r.getCreatedAt()),
						r.getStatus().name()
				))
				.collect(Collectors.toList());
		model.addAttribute("registrations", registrations);
		return "user/profile/registration-history";
	}

	@GetMapping({"/lich-su-giao-dich", "/lich-su-giao-dich.html"})
	public String transactionHistory(Model model, Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		attachCommonProfileModel(model, user);
		var transactions = transactionService.listByUserId(user.getId()).stream()
				.map(t -> new TransactionView(
						t.getCode(),
						t.getDescription(),
						formatVnd(t.getAmount()),
						formatInstant(t.getCreatedAt()),
						t.getStatus().name()
				))
				.collect(Collectors.toList());
		model.addAttribute("transactions", transactions);
		return "user/profile/transaction-history";
	}

	@GetMapping({"/thanh-toan", "/thanh-toan.html"})
	public String payment(Model model, Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		attachCommonProfileModel(model, user);
		if (!model.containsAttribute("paymentForm")) {
			model.addAttribute("paymentForm", new PaymentForm());
		}
		return "user/profile/payment";
	}

	@PostMapping("/thanh-toan")
	public String paymentSubmit(@Valid PaymentForm paymentForm, BindingResult bindingResult, Authentication authentication, Model model) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		if (bindingResult.hasErrors()) {
			attachCommonProfileModel(model, user);
			model.addAttribute("paymentForm", paymentForm);
			return "user/profile/payment";
		}

		String code = paymentForm.orderCode == null || paymentForm.orderCode.isBlank()
				? "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)
				: paymentForm.orderCode.trim();

		BigDecimal amount = paymentForm.amount == null ? BigDecimal.ZERO : paymentForm.amount;
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			amount = BigDecimal.ZERO;
		}

		String desc = paymentForm.description == null || paymentForm.description.isBlank()
				? "Thanh toán"
				: paymentForm.description.trim();

		Transaction tx = new Transaction(code, desc, amount, user);
		tx.setStatus(TransactionStatus.SUCCESS);
		transactionService.save(tx);

		return "redirect:/lich-su-giao-dich";
	}

	private record GeocodeResult(double lat, double lon, String displayName) {
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

	public static class RegisterForm {
		@NotBlank(message = "Vui lòng nhập họ tên")
		@Size(min = 2, max = 50, message = "Họ tên phải từ 2–50 ký tự")
		@jakarta.validation.constraints.Pattern(regexp = "^[\\p{L}][\\p{L}\\s]{1,49}$", message = "Họ tên chỉ được chứa chữ và khoảng trắng")
		private String fullName;

		@NotBlank(message = "Vui lòng nhập email")
		@Email(message = "Email không hợp lệ")
		private String email;

		@NotBlank(message = "Vui lòng nhập số điện thoại")
		@jakarta.validation.constraints.Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
		private String phone;

		@NotBlank(message = "Vui lòng nhập mật khẩu")
		@Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
		private String password;

		@NotBlank(message = "Vui lòng nhập lại mật khẩu")
		private String confirmPassword;

		@AssertTrue(message = "Bạn cần đồng ý điều khoản")
		private boolean terms;

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getConfirmPassword() {
			return confirmPassword;
		}

		public void setConfirmPassword(String confirmPassword) {
			this.confirmPassword = confirmPassword;
		}

		public boolean isTerms() {
			return terms;
		}

		public void setTerms(boolean terms) {
			this.terms = terms;
		}
	}

	public static class ProfileForm {
		@NotBlank(message = "Vui lòng nhập họ tên")
		private String fullName;

		private String phone;

		private String address;

		private Gender gender;

		private java.time.LocalDate dateOfBirth;

		private String avatarUrl;

		public static ProfileForm fromUser(com.example.football_tourament_web.model.entity.AppUser user) {
			ProfileForm f = new ProfileForm();
			f.fullName = user.getFullName();
			f.phone = user.getPhone();
			f.address = user.getAddress();
			f.gender = user.getGender();
			f.dateOfBirth = user.getDateOfBirth();
			f.avatarUrl = user.getAvatarUrl();
			return f;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

		public java.time.LocalDate getDateOfBirth() {
			return dateOfBirth;
		}

		public void setDateOfBirth(java.time.LocalDate dateOfBirth) {
			this.dateOfBirth = dateOfBirth;
		}

		public String getAvatarUrl() {
			return avatarUrl;
		}

		public void setAvatarUrl(String avatarUrl) {
			this.avatarUrl = avatarUrl;
		}
	}

	public static class PaymentForm {
		private String orderCode;

		@NotBlank(message = "Vui lòng nhập mô tả")
		private String description;

		private BigDecimal amount;

		private String method;

		public String getOrderCode() {
			return orderCode;
		}

		public void setOrderCode(String orderCode) {
			this.orderCode = orderCode;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}
	}

	private record TransactionView(String code, String description, String amount, String time, String status) {
	}

	private record RegistrationView(String tournamentName, String teamName, String date, String status) {
	}

	private com.example.football_tourament_web.model.entity.AppUser requireCurrentUser(Authentication authentication) {
		String email = authentication == null ? null : authentication.getName();
		if (email == null || email.isBlank()) {
			return null;
		}
		return userService.findByEmail(email).orElse(null);
	}

	private void attachCommonProfileModel(Model model, com.example.football_tourament_web.model.entity.AppUser user) {
		model.addAttribute("user", user);

		String avatarSrc = user.getAvatarUrl();
		if (avatarSrc == null || avatarSrc.isBlank()) {
			avatarSrc = user.getAvatar();
		}
		if (avatarSrc == null || avatarSrc.isBlank()) {
			avatarSrc = "/assets/figma/avatar.jpg";
		}

		BigDecimal balance = transactionService.calculateBalance(user.getId());
		String balanceText = "Số dư: " + formatVnd(balance);

		model.addAttribute("avatarSrc", avatarSrc);
		model.addAttribute("balanceText", balanceText);
	}

	private String storeAvatarFile(MultipartFile avatarFile) throws Exception {
		if (avatarFile == null || avatarFile.isEmpty()) {
			return null;
		}
		if (avatarFile.getSize() > 2L * 1024 * 1024) {
			throw new AvatarTooLargeException();
		}

		String contentType = avatarFile.getContentType();
		Set<String> allowed = Set.of("image/jpeg", "image/png", "image/webp");
		if (contentType == null || !allowed.contains(contentType)) {
			throw new AvatarInvalidTypeException();
		}

		String ext = switch (contentType) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};

		Path baseDir = Paths.get("uploads", "avatars");
		Files.createDirectories(baseDir);

		String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
		Path target = baseDir.resolve(fileName).normalize();
		if (!target.startsWith(baseDir)) {
			return null;
		}

		try (var in = avatarFile.getInputStream()) {
			Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}

		return "/uploads/avatars/" + fileName;
	}

	private static class AvatarTooLargeException extends RuntimeException {
	}

	private static class AvatarInvalidTypeException extends RuntimeException {
	}

	private static String formatVnd(BigDecimal amount) {
		BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
		long rounded = safe.setScale(0, RoundingMode.HALF_UP).longValue();
		NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
		nf.setGroupingUsed(true);
		return nf.format(rounded) + "đ";
	}

	private static String formatInstant(java.time.Instant instant) {
		if (instant == null) {
			return "";
		}
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
		return fmt.format(instant);
	}
}
