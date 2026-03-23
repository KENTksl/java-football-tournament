package com.example.football_tourament_web.service;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.football_tourament_web.model.entity.Transaction;
import com.example.football_tourament_web.model.enums.TransactionStatus;
import com.example.football_tourament_web.repository.TransactionRepository;

@Service
public class TransactionService {
	private final TransactionRepository transactionRepository;

	public TransactionService(TransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

	@Transactional(readOnly = true)
	public Optional<Transaction> findByCode(String code) {
		return transactionRepository.findByCode(code);
	}

	@Transactional(readOnly = true)
	public List<Transaction> listByUserId(Long userId) {
		return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional(readOnly = true)
	public List<Transaction> listAll() {
		return transactionRepository.findAllWithUserOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public BigDecimal calculateBalance(Long userId) {
		return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
				.map(Transaction::getAmount)
				.filter(a -> a != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Transactional
	public Transaction save(Transaction transaction) {
		return transactionRepository.save(transaction);
	}
}

