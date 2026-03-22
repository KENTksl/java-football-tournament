package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.AppUser;
import com.example.football_tourament_web.repository.AppUserRepository;

@Service
public class UserService {
	private final AppUserRepository userRepository;

	public UserService(AppUserRepository userRepository) {
		this.userRepository = userRepository;
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
}

