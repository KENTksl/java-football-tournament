package com.example.football_tourament_web.repository;

import com.example.football_tourament_web.model.entity.ContactMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
	List<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
	long countByIsReadFalse();
}
