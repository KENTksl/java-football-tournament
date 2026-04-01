package com.example.football_tourament_web.service.common;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
	public String storeUnderUploads(MultipartFile file, String folder) {
		if (file == null || file.isEmpty()) return null;
		String safeFolder = folder == null ? "" : folder.trim();
		if (safeFolder.isEmpty()) return null;

		try {
			String original = file.getOriginalFilename();
			String fileName = UUID.randomUUID() + "_" + (original == null ? "file" : original);
			Path uploadPath = Paths.get("src", "main", "resources", "static", "uploads", safeFolder).toAbsolutePath().normalize();
			Files.createDirectories(uploadPath);

			try (var inputStream = file.getInputStream()) {
				Path filePath = uploadPath.resolve(fileName);
				Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
			}

			return "/uploads/" + safeFolder + "/" + fileName;
		} catch (IOException e) {
			return null;
		}
	}

	public String storeValidatedImageUnderUploads(MultipartFile file, String folder, long maxBytes) {
		if (file == null || file.isEmpty()) return null;
		if (file.getSize() > maxBytes) {
			throw new FileTooLargeException();
		}

		String contentType = file.getContentType();
		Set<String> allowed = Set.of("image/jpeg", "image/png", "image/webp");
		if (contentType == null || !allowed.contains(contentType)) {
			throw new InvalidFileTypeException();
		}

		String ext = switch (contentType) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};

		String safeFolder = folder == null ? "" : folder.trim();
		if (safeFolder.isEmpty()) return null;

		try {
			String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
			Path uploadPath = Paths.get("src", "main", "resources", "static", "uploads", safeFolder).toAbsolutePath().normalize();
			Files.createDirectories(uploadPath);

			try (var inputStream = file.getInputStream()) {
				Path filePath = uploadPath.resolve(fileName).normalize();
				if (!filePath.startsWith(uploadPath)) {
					return null;
				}
				Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
			}

			return "/uploads/" + safeFolder + "/" + fileName;
		} catch (IOException e) {
			return null;
		}
	}

	public static final class FileTooLargeException extends RuntimeException {
	}

	public static final class InvalidFileTypeException extends RuntimeException {
	}
}

