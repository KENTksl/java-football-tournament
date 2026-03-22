package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.model.enums.UserRole;
import com.example.football_tourament_web.repository.AppUserRepository;

@Service
public class UserService {
	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<AppUser> listUsers() {
		return userRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<AppUser> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Transactional(readOnly = true)
	public Optional<AppUser> findById(Long id) {
		return userRepository.findById(id);
	}

	@Transactional
	public AppUser save(AppUser user) {
		return userRepository.save(user);
	}

	@Transactional
	public AppUser registerUser(String fullName, String email, String phone, String rawPassword) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email đã tồn tại");
		}

		AppUser user = new AppUser();
		user.setFullName(fullName);
		user.setEmail(email);
		user.setPhone(phone);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setRole(UserRole.USER);
		return userRepository.save(user);
	}
}

