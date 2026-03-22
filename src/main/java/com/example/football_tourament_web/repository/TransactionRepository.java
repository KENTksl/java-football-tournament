package com.example.football_tourament_web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.football_tourament_web.model.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	Optional<Transaction> findByCode(String code);

	List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}

