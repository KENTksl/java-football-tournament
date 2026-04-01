package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.service.common.FileStorageService;
import com.example.football_tourament_web.service.core.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;

@Service
public class AdminProfileService {
	private final UserService userService;
	private final FileStorageService fileStorageService;

	public AdminProfileService(UserService userService, FileStorageService fileStorageService) {
		this.userService = userService;
		this.fileStorageService = fileStorageService;
	}

	public AppUser findAdmin(Principal principal) {
		String email = principal == null ? null : principal.getName();
		if (email == null || email.isBlank()) return null;
		return userService.findByEmail(email).orElse(null);
	}

	public void saveAdminProfile(
			Principal principal,
			String fullName,
			String phone,
			String address,
			String dob,
			MultipartFile avatarFile
	) {
		AppUser admin = findAdmin(principal);
		if (admin == null) return;

		admin.setFullName(fullName);
		admin.setPhone(phone);
		admin.setAddress(address);
		if (dob != null && !dob.isBlank()) {
			try {
				admin.setDateOfBirth(LocalDate.parse(dob));
			} catch (Exception ignored) {
			}
		}

		if (avatarFile != null && !avatarFile.isEmpty()) {
			String avatarPath = fileStorageService.storeUnderUploads(avatarFile, "avatars");
			if (avatarPath != null && !avatarPath.isBlank()) {
				admin.setAvatar(avatarPath);
				admin.setAvatarUrl(avatarPath);
			}
		}

		userService.save(admin);
	}
}
