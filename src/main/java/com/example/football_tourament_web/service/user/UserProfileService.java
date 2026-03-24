package com.example.football_tourament_web.service.user;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.enums.Gender;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.service.common.FileStorageService;
import com.example.football_tourament_web.service.common.ImageService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TransactionService;
import com.example.football_tourament_web.service.core.UserService;
import com.example.football_tourament_web.service.common.ViewFormatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class UserProfileService {
	private final UserService userService;
	private final TransactionService transactionService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final FileStorageService fileStorageService;
	private final ViewFormatService viewFormatService;
	private final ImageService imageService;

	public UserProfileService(
			UserService userService,
			TransactionService transactionService,
			TournamentRegistrationService tournamentRegistrationService,
			FileStorageService fileStorageService,
			ViewFormatService viewFormatService,
			ImageService imageService
	) {
		this.userService = userService;
		this.transactionService = transactionService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.fileStorageService = fileStorageService;
		this.viewFormatService = viewFormatService;
		this.imageService = imageService;
	}

	public AppUser requireCurrentUser(Authentication authentication) {
		String email = authentication == null ? null : authentication.getName();
		if (email == null || email.isBlank()) {
			return null;
		}
		return userService.findByEmail(email).orElse(null);
	}

	public void attachCommonProfileModel(Model model, AppUser user) {
		model.addAttribute("user", user);

		String avatarSrc = user.getAvatarUrl();
		if (avatarSrc == null || avatarSrc.isBlank()) {
			avatarSrc = user.getAvatar();
		}
		model.addAttribute("avatarSrc", imageService.resolveUserAvatarUrl(avatarSrc));

		BigDecimal balance = transactionService.calculateBalance(user.getId());
		String balanceText = "Số dư: " + viewFormatService.formatMoney(balance);
		model.addAttribute("balanceText", balanceText);
	}

	public String handleUpdateProfile(
			@Valid ProfileForm profileForm,
			BindingResult bindingResult,
			MultipartFile avatarFile,
			Authentication authentication,
			Model model
	) {
		AppUser user = requireCurrentUser(authentication);
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
				String stored = fileStorageService.storeValidatedImageUnderUploads(avatarFile, "avatars", 2L * 1024 * 1024);
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
		} catch (FileStorageService.FileTooLargeException ex) {
			return "redirect:/ca-nhan?avatarTooLarge";
		} catch (FileStorageService.InvalidFileTypeException ex) {
			return "redirect:/ca-nhan?avatarInvalidType";
		} catch (Exception ex) {
			return "redirect:/ca-nhan?avatarError";
		}
	}

	public List<RegistrationView> listRegistrations(AppUser user) {
		return tournamentRegistrationService.listByUserId(user.getId()).stream()
				.map(r -> new RegistrationView(
						r.getTournament().getName(),
						r.getTeam().getName(),
						viewFormatService.formatDateTime(r.getCreatedAt()),
						r.getStatus().name()
				))
				.toList();
	}

	public List<TransactionHistoryRow> listTransactions(AppUser user) {
		return transactionService.listByUserId(user.getId()).stream()
				.map(t -> new TransactionHistoryRow(
						viewFormatService.formatDateTime(t.getCreatedAt()),
						viewFormatService.formatMoney(t.getAmount()),
						transactionStatusLabel(t.getStatus()),
						transactionStatusClass(t.getStatus())
				))
				.toList();
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

	public static class ProfileForm {
		@NotBlank(message = "Vui lòng nhập họ tên")
		private String fullName;

		private String phone;

		private String address;

		private Gender gender;

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		private LocalDate dateOfBirth;

		private String avatarUrl;

		public static ProfileForm fromUser(AppUser user) {
			ProfileForm f = new ProfileForm();
			f.fullName = user.getFullName();
			f.phone = user.getPhone();
			f.address = user.getAddress();
			f.gender = user.getGender();
			f.dateOfBirth = user.getDateOfBirth();
			f.avatarUrl = user.getAvatarUrl();
			return f;
		}

		public String getFullName() { return fullName; }
		public void setFullName(String fullName) { this.fullName = fullName; }
		public String getPhone() { return phone; }
		public void setPhone(String phone) { this.phone = phone; }
		public String getAddress() { return address; }
		public void setAddress(String address) { this.address = address; }
		public Gender getGender() { return gender; }
		public void setGender(Gender gender) { this.gender = gender; }
		public LocalDate getDateOfBirth() { return dateOfBirth; }
		public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
		public String getAvatarUrl() { return avatarUrl; }
		public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
	}

	public record TransactionHistoryRow(String time, String amount, String statusLabel, String statusClass) {
	}

	public record RegistrationView(String tournamentName, String teamName, String date, String status) {
	}
}
