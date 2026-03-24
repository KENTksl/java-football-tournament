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
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.dto.NewsItem;
import com.example.football_tourament_web.model.entity.Match;
import com.example.football_tourament_web.model.entity.Player;
import com.example.football_tourament_web.model.entity.Team;
import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.entity.ContactMessage;
import com.example.football_tourament_web.model.enums.Gender;
import com.example.football_tourament_web.model.enums.MatchStatus;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.repository.MatchRepository;
import com.example.football_tourament_web.repository.PlayerRepository;
import com.example.football_tourament_web.service.CbsSportsNewsService;
import com.example.football_tourament_web.service.ContactMessageService;
import com.example.football_tourament_web.service.MomoPaymentService;
import com.example.football_tourament_web.service.TeamService;
import com.example.football_tourament_web.service.TournamentService;
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
	private final MomoPaymentService momoPaymentService;
	private final TournamentService tournamentService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final TeamService teamService;
	private final MatchRepository matchRepository;
	private final PlayerRepository playerRepository;
	private final ContactMessageService contactMessageService;
	private final HttpClient httpClient;
	private static final Pattern LAT_PATTERN = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern LON_PATTERN = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"");

	public HomeController(
			CbsSportsNewsService cbsSportsNewsService,
			UserService userService,
			TransactionService transactionService,
			MomoPaymentService momoPaymentService,
			TournamentService tournamentService,
			TournamentRegistrationService tournamentRegistrationService,
			TeamService teamService,
			MatchRepository matchRepository,
			PlayerRepository playerRepository,
			ContactMessageService contactMessageService
	) {
		this.cbsSportsNewsService = cbsSportsNewsService;
		this.userService = userService;
		this.transactionService = transactionService;
		this.momoPaymentService = momoPaymentService;
		this.tournamentService = tournamentService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.teamService = teamService;
		this.matchRepository = matchRepository;
		this.playerRepository = playerRepository;
		this.contactMessageService = contactMessageService;
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(10))
				.build();
	}

	@GetMapping({"/", "/home"})
	public String home(Model model) {
		List<FeaturedTournamentView> featuredTournaments = tournamentService.listTournaments().stream()
				.sorted((a, b) -> {
					int sa = tournamentStatusPriority(a.getStatus());
					int sb = tournamentStatusPriority(b.getStatus());
					if (sa != sb) {
						return Integer.compare(sa, sb);
					}
					if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
						return b.getCreatedAt().compareTo(a.getCreatedAt());
					}
					return Long.compare(b.getId() == null ? 0 : b.getId(), a.getId() == null ? 0 : a.getId());
				})
				.limit(3)
				.map(t -> new FeaturedTournamentView(
						t.getId(),
						t.getName(),
						t.getImageUrl(),
						featuredTournamentMeta(t),
						t.getStatus() == null ? "Đang cập nhật" : tournamentStatusLabel(t.getStatus())
				))
				.collect(Collectors.toList());
		model.addAttribute("featuredTournaments", featuredTournaments);
		model.addAttribute("hasFeaturedTournaments", !featuredTournaments.isEmpty());
		return "user/home/index";
	}

	private static int tournamentStatusPriority(TournamentStatus status) {
		if (status == null) {
			return 10;
		}
		return switch (status) {
			case LIVE -> 0;
			case UPCOMING -> 1;
			case FINISHED -> 2;
		};
	}

	private static String featuredTournamentMeta(Tournament t) {
		if (t == null) {
			return "";
		}
		String teamLimit = t.getTeamLimit() == null ? null : (t.getTeamLimit() + " đội");
		String pitch = t.getPitchType() == null ? null : switch (t.getPitchType()) {
			case PITCH_5 -> "Sân 5";
			case PITCH_7 -> "Sân 7";
			case PITCH_11 -> "Sân 11";
		};
		String mode = t.getMode() == null ? null : switch (t.getMode()) {
			case KNOCKOUT -> "Knockout";
			case GROUP_STAGE -> "Vòng bảng";
		};

		List<String> parts = new ArrayList<>();
		if (teamLimit != null) parts.add(teamLimit);
		if (pitch != null) parts.add(pitch);
		if (mode != null) parts.add(mode);
		return String.join(" • ", parts);
	}

	private record FeaturedTournamentView(Long id, String name, String imageUrl, String meta, String statusLabel) {
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

	@PostMapping("/lien-he")
	public String submitContactMessage(
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "email", required = false) String email,
			@RequestParam(name = "message", required = false) String message
	) {
		String n = name == null ? "" : name.trim();
		String e = email == null ? "" : email.trim();
		String m = message == null ? "" : message.trim();

		if (n.isBlank() || e.isBlank() || m.isBlank()) {
			return "redirect:/lien-he?error";
		}

		try {
			ContactMessage cm = new ContactMessage();
			cm.setName(n);
			cm.setEmail(e);
			cm.setMessage(m);
			contactMessageService.save(cm);
		} catch (Exception ex) {
			return "redirect:/lien-he?error";
		}
		return "redirect:/lien-he?sent";
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
			bindingResult.rejectValue("email", "email.exists", ex.getMessage());
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
	public String teamInfo(
			@RequestParam(name = "teamId", required = false) Long teamId,
			@RequestParam(name = "tournamentId", required = false) Long tournamentId,
			@RequestParam(name = "tab", required = false) String tab,
			@RequestParam(name = "create", required = false) String create,
			@RequestParam(name = "edit", required = false) String edit,
			Model model,
			Authentication authentication
	) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		attachCommonProfileModel(model, user);

		List<TeamCardView> teams = teamService.listByCaptain(user.getId()).stream()
				.map(t -> new TeamCardView(
						t.getId(),
						t.getName(),
						t.getLogoUrl(),
						(int) playerRepository.countByTeamId(t.getId())
				))
				.collect(Collectors.toList());
		model.addAttribute("teams", teams);

		boolean canCreateTeam = teams.size() < 2;
		model.addAttribute("canCreateTeam", canCreateTeam);

		Long selectedTeamId = teamId;
		if (selectedTeamId == null && !teams.isEmpty()) {
			selectedTeamId = teams.get(0).id();
		}

		boolean isCreate = create != null;
		boolean isEdit = edit != null;

		if (isCreate && !canCreateTeam) {
			return "redirect:/thong-tin-doi?limitReached";
		}

		if (selectedTeamId != null) {
			var selectedOpt = teamService.findById(selectedTeamId)
					.filter(t -> t.getCaptain() != null && t.getCaptain().getId().equals(user.getId()));
			if (selectedOpt.isPresent()) {
				Team selectedTeam = selectedOpt.get();
				List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(selectedTeam.getId());
				model.addAttribute("selectedTeam", selectedTeam);
				model.addAttribute("members", members);

				if (isEdit) {
					model.addAttribute("editTeamId", selectedTeam.getId());
					if (!model.containsAttribute("teamForm")) {
						TeamCreateForm f = new TeamCreateForm();
						f.setTeamName(selectedTeam.getName());
						model.addAttribute("teamForm", f);
					}
					ensureMemberSlots(model, members);
				}

				if (!isCreate && !isEdit) {
					model.addAttribute("activeTeamTab", tab == null || tab.isBlank() ? "info" : tab);
					attachTeamDetailModel(model, selectedTeam, tournamentId);
				}
			}
		}

		model.addAttribute("isCreate", isCreate);
		model.addAttribute("isEdit", isEdit);
		if (isCreate && !model.containsAttribute("teamForm")) {
			model.addAttribute("teamForm", new TeamCreateForm());
		}
		if (isCreate) {
			ensureMemberSlots(model, List.of());
		}
		return "user/profile/team-info";
	}

	@PostMapping("/thong-tin-doi/tao-doi")
	public String createTeam(
			@Valid TeamCreateForm teamForm,
			BindingResult bindingResult,
			@RequestParam(name = "teamLogo", required = false) MultipartFile teamLogo,
			@RequestParam(name = "memberName", required = false) List<String> memberNames,
			@RequestParam(name = "memberJersey", required = false) List<Integer> memberJerseys,
			@RequestParam(name = "memberAvatar", required = false) MultipartFile[] memberAvatars,
			Authentication authentication,
			Model model
	) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		if (teamService.countByCaptain(user.getId()) >= 2) {
			return "redirect:/thong-tin-doi?limitReached";
		}

		if (bindingResult.hasErrors()) {
			return renderCreateTeamError(model, user, teamForm);
		}

		String name = teamForm.teamName == null ? "" : teamForm.teamName.trim();
		if (name.isBlank()) {
			bindingResult.rejectValue("teamName", "teamName.blank", "Vui lòng nhập tên đội");
			return renderCreateTeamError(model, user, teamForm);
		}

		if (teamService.findByName(name).isPresent()) {
			bindingResult.rejectValue("teamName", "teamName.exists", "Tên đội đã tồn tại");
			return renderCreateTeamError(model, user, teamForm);
		}

		String logoUrl = null;
		try {
			if (teamLogo != null && !teamLogo.isEmpty()) {
				logoUrl = storeImageFile(teamLogo, "teams", 2L * 1024 * 1024);
			}
		} catch (AvatarTooLargeException ex) {
			return "redirect:/thong-tin-doi?create=1&logoTooLarge";
		} catch (AvatarInvalidTypeException ex) {
			return "redirect:/thong-tin-doi?create=1&logoInvalidType";
		} catch (Exception ex) {
			return "redirect:/thong-tin-doi?create=1&logoError";
		}

		Team team = new Team(name);
		team.setCaptain(user);
		team.setLogoUrl(logoUrl);
		team = teamService.save(team);

		List<Player> players = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String n = memberNames != null && i < memberNames.size() ? memberNames.get(i) : null;
			if (n == null || n.trim().isBlank()) {
				continue;
			}

			Integer jersey = memberJerseys != null && i < memberJerseys.size() ? memberJerseys.get(i) : null;
			if (jersey != null && (jersey < 0 || jersey > 999)) {
				jersey = null;
			}

			String avatarUrl = null;
			try {
				MultipartFile avatarFile = memberAvatars != null && i < memberAvatars.length ? memberAvatars[i] : null;
				if (avatarFile != null && !avatarFile.isEmpty()) {
					avatarUrl = storeImageFile(avatarFile, "players", 2L * 1024 * 1024);
				}
			} catch (Exception ex) {
				avatarUrl = null;
			}

			Player p = new Player(n.trim(), team);
			p.setJerseyNumber(jersey);
			p.setRole("Cầu thủ");
			p.setAvatarUrl(avatarUrl);
			players.add(p);
		}
		if (!players.isEmpty()) {
			playerRepository.saveAll(players);
		}

		return "redirect:/thong-tin-doi?teamId=" + team.getId();
	}

	@PostMapping("/thong-tin-doi/cap-nhat-doi")
	@Transactional
	public String updateTeam(
			@RequestParam("teamId") Long teamId,
			@Valid TeamCreateForm teamForm,
			BindingResult bindingResult,
			@RequestParam(name = "existingLogoUrl", required = false) String existingLogoUrl,
			@RequestParam(name = "teamLogo", required = false) MultipartFile teamLogo,
			@RequestParam(name = "memberName", required = false) List<String> memberNames,
			@RequestParam(name = "memberJersey", required = false) List<Integer> memberJerseys,
			@RequestParam(name = "existingMemberAvatar", required = false) List<String> existingMemberAvatars,
			@RequestParam(name = "memberAvatar", required = false) MultipartFile[] memberAvatars,
			Authentication authentication,
			Model model
	) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		var teamOpt = teamService.findById(teamId)
				.filter(t -> t.getCaptain() != null && t.getCaptain().getId().equals(user.getId()));
		if (teamOpt.isEmpty()) {
			return "redirect:/thong-tin-doi";
		}
		Team team = teamOpt.get();

		if (bindingResult.hasErrors()) {
			return renderEditTeamError(model, user, team, teamForm);
		}

		String name = teamForm.teamName == null ? "" : teamForm.teamName.trim();
		if (name.isBlank()) {
			bindingResult.rejectValue("teamName", "teamName.blank", "Vui lòng nhập tên đội");
			return renderEditTeamError(model, user, team, teamForm);
		}

		var existingByName = teamService.findByName(name);
		if (existingByName.isPresent() && !existingByName.get().getId().equals(team.getId())) {
			bindingResult.rejectValue("teamName", "teamName.exists", "Tên đội đã tồn tại");
			return renderEditTeamError(model, user, team, teamForm);
		}

		String nextLogoUrl = existingLogoUrl;
		try {
			if (teamLogo != null && !teamLogo.isEmpty()) {
				nextLogoUrl = storeImageFile(teamLogo, "teams", 2L * 1024 * 1024);
			}
		} catch (AvatarTooLargeException ex) {
			return "redirect:/thong-tin-doi?teamId=" + team.getId() + "&edit=1&logoTooLarge";
		} catch (AvatarInvalidTypeException ex) {
			return "redirect:/thong-tin-doi?teamId=" + team.getId() + "&edit=1&logoInvalidType";
		} catch (Exception ex) {
			return "redirect:/thong-tin-doi?teamId=" + team.getId() + "&edit=1&logoError";
		}

		team.setName(name);
		team.setLogoUrl(nextLogoUrl);
		teamService.save(team);

		playerRepository.deleteByTeamId(team.getId());

		List<Player> players = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String n = memberNames != null && i < memberNames.size() ? memberNames.get(i) : null;
			if (n == null || n.trim().isBlank()) {
				continue;
			}

			Integer jersey = memberJerseys != null && i < memberJerseys.size() ? memberJerseys.get(i) : null;
			if (jersey != null && (jersey < 0 || jersey > 999)) {
				jersey = null;
			}

			String avatarUrl = existingMemberAvatars != null && i < existingMemberAvatars.size()
					? existingMemberAvatars.get(i)
					: null;
			try {
				MultipartFile avatarFile = memberAvatars != null && i < memberAvatars.length ? memberAvatars[i] : null;
				if (avatarFile != null && !avatarFile.isEmpty()) {
					avatarUrl = storeImageFile(avatarFile, "players", 2L * 1024 * 1024);
				}
			} catch (Exception ex) {
				avatarUrl = null;
			}

			Player p = new Player(n.trim(), team);
			p.setJerseyNumber(jersey);
			p.setRole("Cầu thủ");
			p.setAvatarUrl(avatarUrl);
			players.add(p);
		}
		if (!players.isEmpty()) {
			playerRepository.saveAll(players);
		}

		return "redirect:/thong-tin-doi?teamId=" + team.getId();
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
				.map(t -> new TransactionHistoryRow(
						formatInstant(t.getCreatedAt()),
						formatVnd(t.getAmount()),
						transactionStatusLabel(t.getStatus()),
						transactionStatusClass(t.getStatus())
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
		model.addAttribute("paymentHasResult", false);
		model.addAttribute("paymentSuccess", false);
		if (!model.containsAttribute("paymentForm")) {
			model.addAttribute("paymentForm", new PaymentForm());
		}
		return "user/profile/payment";
	}

	@GetMapping("/thanh-toan/ket-qua")
	public String paymentResult(@RequestParam(name = "code", required = false) String code, Model model, Authentication authentication) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		if (code == null || code.isBlank()) {
			return "redirect:/thanh-toan";
		}

		Transaction tx = transactionService.findByCode(code.trim()).orElse(null);
		if (tx == null || tx.getUser() == null || !user.getId().equals(tx.getUser().getId())) {
			return "redirect:/thanh-toan";
		}

		attachCommonProfileModel(model, user);
		model.addAttribute("paymentHasResult", true);
		model.addAttribute("paymentSuccess", tx.getStatus() == TransactionStatus.SUCCESS);
		model.addAttribute("paymentResultAmount", formatVnd(tx.getAmount()));
		model.addAttribute("paymentResultStatusLabel", transactionStatusLabel(tx.getStatus()));
		model.addAttribute("paymentResultStatusClass", transactionStatusClass(tx.getStatus()));
		model.addAttribute("paymentResultIconClass", paymentResultIconClass(tx.getStatus()));
		model.addAttribute("paymentResultIconChar", paymentResultIconChar(tx.getStatus()));
		model.addAttribute("paymentResultTitle", paymentResultTitle(tx.getStatus()));
		return "user/profile/payment";
	}

	@PostMapping("/thanh-toan")
	public String paymentSubmit(@Valid PaymentForm paymentForm, BindingResult bindingResult, Authentication authentication, Model model) {
		var user = requireCurrentUser(authentication);
		if (user == null) {
			return "redirect:/dang-nhap";
		}

		Integer option = paymentForm.amountOption;
		List<Integer> allowed = List.of(10, 20, 50, 100);
		if (option == null || !allowed.contains(option)) {
			bindingResult.rejectValue("amountOption", "amountOption.invalid", "Vui lòng chọn số tiền 10k/20k/50k/100k");
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

	@GetMapping({"/thanh-toan/momo/callback", "/order/momo-return"})
	public String momoCallback(
			@RequestParam(name = "orderId", required = false) String orderId,
			@RequestParam(name = "requestId", required = false) String requestId,
			@RequestParam(name = "errorCode", required = false) String errorCode,
			@RequestParam(name = "resultCode", required = false) String resultCode
	) {
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

	@PostMapping({"/thanh-toan/momo/notify", "/order/momo-notify"})
	@ResponseBody
	public ResponseEntity<String> momoNotify(
			@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> body,
			@RequestParam(name = "orderId", required = false) String orderIdParam,
			@RequestParam(name = "requestId", required = false) String requestIdParam,
			@RequestParam(name = "errorCode", required = false) String errorCodeParam,
			@RequestParam(name = "resultCode", required = false) String resultCodeParam
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
		private String fullName;

		@NotBlank(message = "Vui lòng nhập email")
		@Email(message = "Email không hợp lệ")
		private String email;

		@NotBlank(message = "Vui lòng nhập số điện thoại")
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

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
		private Integer amountOption;

		public Integer getAmountOption() {
			return amountOption;
		}

		public void setAmountOption(Integer amountOption) {
			this.amountOption = amountOption;
		}
	}

	public static class TeamCreateForm {
		@NotBlank(message = "Vui lòng nhập tên đội")
		private String teamName;

		public String getTeamName() {
			return teamName;
		}

		public void setTeamName(String teamName) {
			this.teamName = teamName;
		}
	}

	private record TransactionHistoryRow(String time, String amount, String statusLabel, String statusClass) {
	}

	private record RegistrationView(String tournamentName, String teamName, String date, String status) {
	}

	private record TeamCardView(Long id, String name, String logoUrl, int memberCount) {
	}

	private record MemberSlot(String name, Integer jerseyNumber, String avatarUrl) {
	}

	private record ScheduleRow(String date, String time, String tournamentName, String opponentName, String round, String status) {
	}

	private record ResultRow(String date, String tournamentName, String opponentName, String score, String round) {
	}

	private record TournamentOption(Long id, String name) {
	}

	private record SeasonCard(Long tournamentId, String tournamentName, String statusLabel, String statusClass, String achievementLabel, String achievementClass) {
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

	private void attachTeamDetailModel(Model model, Team team, Long tournamentId) {
		var registrations = tournamentRegistrationService.listApprovedByTeamId(team.getId());
		boolean hasRegistrations = registrations != null && !registrations.isEmpty();
		model.addAttribute("hasTeamRegistrations", hasRegistrations);

		if (!hasRegistrations) {
			model.addAttribute("scheduleRows", List.of());
			model.addAttribute("resultRows", List.of());
			model.addAttribute("recentResults", List.of());
			model.addAttribute("seasonCards", List.of());
			model.addAttribute("tournamentOptions", List.of());
			model.addAttribute("selectedTournamentId", null);
			model.addAttribute("analysisWin", 0);
			model.addAttribute("analysisDraw", 0);
			model.addAttribute("analysisLoss", 0);
			model.addAttribute("analysisLabels", List.of());
			model.addAttribute("analysisGoalsFor", List.of());
			model.addAttribute("analysisGoalsAgainst", List.of());
			return;
		}

		List<TournamentOption> tournamentOptions = registrations.stream()
				.map(r -> new TournamentOption(r.getTournament().getId(), r.getTournament().getName()))
				.distinct()
				.collect(Collectors.toList());
		model.addAttribute("tournamentOptions", tournamentOptions);

		List<Long> tournamentIds = registrations.stream()
				.map(r -> r.getTournament().getId())
				.distinct()
				.collect(Collectors.toList());

		Long selectedTournamentId = tournamentId != null && tournamentIds.contains(tournamentId) ? tournamentId : null;
		model.addAttribute("selectedTournamentId", selectedTournamentId);

		List<Match> matches;
		if (selectedTournamentId != null) {
			matches = matchRepository.findByTeamIdAndTournamentIdWithDetails(team.getId(), selectedTournamentId);
		} else {
			matches = matchRepository.findByTeamIdWithDetails(team.getId()).stream()
					.filter(m -> m.getTournament() != null && m.getTournament().getId() != null && tournamentIds.contains(m.getTournament().getId()))
					.collect(Collectors.toList());
		}

		List<ScheduleRow> scheduleRows = matches.stream()
				.filter(m -> m.getScheduledAt() != null && m.getStatus() != MatchStatus.FINISHED)
				.map(m -> new ScheduleRow(
						formatDate(m.getScheduledAt()),
						formatTime(m.getScheduledAt()),
						m.getTournament().getName(),
						opponentName(team.getId(), m),
						m.getRoundName() == null ? "" : m.getRoundName(),
						matchStatusLabel(m.getStatus())
				))
				.collect(Collectors.toList());
		model.addAttribute("scheduleRows", scheduleRows);

		List<ResultRow> resultRows = matches.stream()
				.filter(m -> m.getScheduledAt() != null && m.getStatus() == MatchStatus.FINISHED)
				.sorted((a, b) -> b.getScheduledAt().compareTo(a.getScheduledAt()))
				.map(m -> new ResultRow(
						formatDate(m.getScheduledAt()),
						m.getTournament().getName(),
						opponentName(team.getId(), m),
						scoreText(team.getId(), m),
						m.getRoundName() == null ? "" : m.getRoundName()
				))
				.collect(Collectors.toList());
		model.addAttribute("resultRows", resultRows);

		model.addAttribute("recentResults", resultRows.stream().limit(6).collect(Collectors.toList()));

		List<SeasonCard> seasonCards = registrations.stream()
				.map(r -> {
					var t = r.getTournament();
					String statusLabel = tournamentStatusLabel(t.getStatus());
					String statusClass = tournamentStatusClass(t.getStatus());

					String achievementLabel = "Đã tham gia";
					String achievementClass = "badge--joined";

					if (t.getStatus() == TournamentStatus.FINISHED) {
						if (t.getWinner() != null && t.getWinner().getId() != null && t.getWinner().getId().equals(team.getId())) {
							achievementLabel = "Vô địch";
							achievementClass = "badge--champion";
						} else {
							boolean runnerUp = isRunnerUp(team.getId(), matches, t.getId());
							if (runnerUp) {
								achievementLabel = "Á quân";
								achievementClass = "badge--runnerup";
							}
						}
					}

					return new SeasonCard(t.getId(), t.getName(), statusLabel, statusClass, achievementLabel, achievementClass);
				})
				.collect(Collectors.toList());
		model.addAttribute("seasonCards", seasonCards);

		int win = 0;
		int draw = 0;
		int loss = 0;
		List<String> labels = new ArrayList<>();
		List<Integer> goalsFor = new ArrayList<>();
		List<Integer> goalsAgainst = new ArrayList<>();

		List<Match> finished = matches.stream()
				.filter(m -> m.getStatus() == MatchStatus.FINISHED)
				.sorted((a, b) -> a.getScheduledAt() == null || b.getScheduledAt() == null ? 0 : a.getScheduledAt().compareTo(b.getScheduledAt()))
				.collect(Collectors.toList());

		for (Match m : finished) {
			Integer gf = goalsFor(team.getId(), m);
			Integer ga = goalsAgainst(team.getId(), m);
			if (gf != null && ga != null) {
				if (gf > ga) {
					win++;
				} else if (gf.equals(ga)) {
					draw++;
				} else {
					loss++;
				}
			}
		}

		List<Match> lastForChart = finished.stream()
				.sorted((a, b) -> b.getScheduledAt() == null || a.getScheduledAt() == null ? 0 : b.getScheduledAt().compareTo(a.getScheduledAt()))
				.limit(8)
				.collect(Collectors.toList());
		java.util.Collections.reverse(lastForChart);

		for (Match m : lastForChart) {
			String label = m.getRoundName() != null && !m.getRoundName().isBlank()
					? m.getRoundName()
					: (m.getScheduledAt() == null ? "Trận" : formatDate(m.getScheduledAt()));
			labels.add(label);
			goalsFor.add(safeInt(goalsFor(team.getId(), m)));
			goalsAgainst.add(safeInt(goalsAgainst(team.getId(), m)));
		}

		model.addAttribute("analysisWin", win);
		model.addAttribute("analysisDraw", draw);
		model.addAttribute("analysisLoss", loss);
		model.addAttribute("analysisLabels", labels);
		model.addAttribute("analysisGoalsFor", goalsFor);
		model.addAttribute("analysisGoalsAgainst", goalsAgainst);
	}

	private static String formatDate(LocalDateTime dt) {
		if (dt == null) {
			return "";
		}
		return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
	}

	private static String formatTime(LocalDateTime dt) {
		if (dt == null) {
			return "";
		}
		return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
	}

	private static String matchStatusLabel(MatchStatus status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case SCHEDULED -> "Sắp diễn ra";
			case LIVE -> "Đang diễn ra";
			case FINISHED -> "Đã kết thúc";
		};
	}

	private static String tournamentStatusLabel(TournamentStatus status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case UPCOMING -> "Sắp diễn ra";
			case LIVE -> "Đang diễn ra";
			case FINISHED -> "Đã kết thúc";
		};
	}

	private static String tournamentStatusClass(TournamentStatus status) {
		if (status == null) {
			return "badge--muted";
		}
		return switch (status) {
			case UPCOMING -> "badge--upcoming";
			case LIVE -> "badge--live";
			case FINISHED -> "badge--finished";
		};
	}

	private static String transactionStatusLabel(TransactionStatus status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case PENDING -> "Đang chờ thanh toán";
			case SUCCESS -> "Thành công";
			case FAILED -> "Thất bại";
		};
	}

	private static String transactionStatusClass(TransactionStatus status) {
		if (status == null) {
			return "badge--muted";
		}
		return switch (status) {
			case PENDING -> "badge--pending";
			case SUCCESS -> "badge--success";
			case FAILED -> "badge--failed";
		};
	}

	private static String paymentResultIconClass(TransactionStatus status) {
		if (status == null) {
			return "pay-result__icon--pending";
		}
		return switch (status) {
			case SUCCESS -> "pay-result__icon--success";
			case FAILED -> "pay-result__icon--failed";
			case PENDING -> "pay-result__icon--pending";
		};
	}

	private static String paymentResultIconChar(TransactionStatus status) {
		if (status == null) {
			return "…";
		}
		return switch (status) {
			case SUCCESS -> "✓";
			case FAILED -> "!";
			case PENDING -> "…";
		};
	}

	private static String paymentResultTitle(TransactionStatus status) {
		if (status == TransactionStatus.SUCCESS) {
			return "Cảm ơn bạn đã thanh toán!";
		}
		return "Trạng thái thanh toán";
	}

	private static String opponentName(Long teamId, Match m) {
		if (m == null || teamId == null) {
			return "";
		}
		if (m.getHomeTeam() != null && teamId.equals(m.getHomeTeam().getId())) {
			return m.getAwayTeam() == null ? "" : m.getAwayTeam().getName();
		}
		return m.getHomeTeam() == null ? "" : m.getHomeTeam().getName();
	}

	private static Integer goalsFor(Long teamId, Match m) {
		if (m == null || teamId == null) {
			return null;
		}
		if (m.getHomeTeam() != null && teamId.equals(m.getHomeTeam().getId())) {
			return m.getHomeScore();
		}
		if (m.getAwayTeam() != null && teamId.equals(m.getAwayTeam().getId())) {
			return m.getAwayScore();
		}
		return null;
	}

	private static Integer goalsAgainst(Long teamId, Match m) {
		if (m == null || teamId == null) {
			return null;
		}
		if (m.getHomeTeam() != null && teamId.equals(m.getHomeTeam().getId())) {
			return m.getAwayScore();
		}
		if (m.getAwayTeam() != null && teamId.equals(m.getAwayTeam().getId())) {
			return m.getHomeScore();
		}
		return null;
	}

	private static String scoreText(Long teamId, Match m) {
		Integer gf = goalsFor(teamId, m);
		Integer ga = goalsAgainst(teamId, m);
		if (gf == null || ga == null) {
			return "-";
		}
		return gf + " - " + ga;
	}

	private static boolean isRunnerUp(Long teamId, List<Match> matches, Long tournamentId) {
		if (teamId == null || matches == null || tournamentId == null) {
			return false;
		}

		Match finalMatch = matches.stream()
				.filter(m -> m.getTournament() != null && tournamentId.equals(m.getTournament().getId()))
				.filter(m -> m.getRoundName() != null)
				.filter(m -> {
					String rn = m.getRoundName().toLowerCase(Locale.ROOT);
					return rn.contains("chung kết") || rn.contains("final");
				})
				.findFirst()
				.orElse(null);
		if (finalMatch == null) {
			return false;
		}
		Integer gf = goalsFor(teamId, finalMatch);
		Integer ga = goalsAgainst(teamId, finalMatch);
		if (gf == null || ga == null) {
			return false;
		}
		return gf < ga;
	}

	private static int safeInt(Integer value) {
		return value == null ? 0 : value;
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

		Path baseDir = uploadBaseDir("avatars");
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

	private String storeImageFile(MultipartFile file, String folder, long maxBytes) throws Exception {
		if (file == null || file.isEmpty()) {
			return null;
		}
		if (file.getSize() > maxBytes) {
			throw new AvatarTooLargeException();
		}

		String contentType = file.getContentType();
		Set<String> allowed = Set.of("image/jpeg", "image/png", "image/webp");
		if (contentType == null || !allowed.contains(contentType)) {
			throw new AvatarInvalidTypeException();
		}

		String ext = switch (contentType) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};

		Path baseDir = uploadBaseDir(folder);
		Files.createDirectories(baseDir);

		String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
		Path target = baseDir.resolve(fileName).normalize();
		if (!target.startsWith(baseDir)) {
			return null;
		}

		try (var in = file.getInputStream()) {
			Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}

		return "/uploads/" + folder + "/" + fileName;
	}

	private Path uploadBaseDir(String folder) {
		return Paths.get("src", "main", "resources", "static", "uploads", folder).toAbsolutePath().normalize();
	}

	private static class AvatarTooLargeException extends RuntimeException {
	}

	private static class AvatarInvalidTypeException extends RuntimeException {
	}

	private String renderCreateTeamError(Model model, com.example.football_tourament_web.model.entity.AppUser user, TeamCreateForm teamForm) {
		attachCommonProfileModel(model, user);
		List<TeamCardView> teams = teamService.listByCaptain(user.getId()).stream()
				.map(t -> new TeamCardView(
						t.getId(),
						t.getName(),
						t.getLogoUrl(),
						(int) playerRepository.countByTeamId(t.getId())
				))
				.collect(Collectors.toList());
		model.addAttribute("teams", teams);
		model.addAttribute("canCreateTeam", teams.size() < 2);
		model.addAttribute("isCreate", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("teamForm", teamForm);
		ensureMemberSlots(model, List.of());
		return "user/profile/team-info";
	}

	private String renderEditTeamError(Model model, com.example.football_tourament_web.model.entity.AppUser user, Team team, TeamCreateForm teamForm) {
		attachCommonProfileModel(model, user);
		List<TeamCardView> teams = teamService.listByCaptain(user.getId()).stream()
				.map(t -> new TeamCardView(
						t.getId(),
						t.getName(),
						t.getLogoUrl(),
						(int) playerRepository.countByTeamId(t.getId())
				))
				.collect(Collectors.toList());
		model.addAttribute("teams", teams);
		model.addAttribute("canCreateTeam", teams.size() < 2);
		model.addAttribute("selectedTeam", team);
		List<Player> members = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId());
		model.addAttribute("members", members);
		List<MemberSlot> slots = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Player p = i < members.size() ? members.get(i) : null;
			slots.add(new MemberSlot(
					p == null ? "" : p.getFullName(),
					p == null ? null : p.getJerseyNumber(),
					p == null ? null : p.getAvatarUrl()
			));
		}
		model.addAttribute("memberSlots", slots);
		model.addAttribute("isCreate", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("editTeamId", team.getId());
		model.addAttribute("teamForm", teamForm);
		return "user/profile/team-info";
	}

	private void ensureMemberSlots(Model model, List<Player> members) {
		if (model.containsAttribute("memberSlots")) {
			return;
		}
		List<MemberSlot> slots = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Player p = i < members.size() ? members.get(i) : null;
			slots.add(new MemberSlot(
					p == null ? "" : p.getFullName(),
					p == null ? null : p.getJerseyNumber(),
					p == null ? null : p.getAvatarUrl()
			));
		}
		model.addAttribute("memberSlots", slots);
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
