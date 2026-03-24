package com.example.football_tourament_web.service;

import com.example.football_tourament_web.model.entity.ContactMessage;
import com.example.football_tourament_web.repository.ContactMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactMessageService {
	private final ContactMessageRepository contactMessageRepository;

	public ContactMessageService(ContactMessageRepository contactMessageRepository) {
		this.contactMessageRepository = contactMessageRepository;
	}

	@Transactional
	public ContactMessage save(ContactMessage message) {
		return contactMessageRepository.save(message);
	}

	@Transactional(readOnly = true)
	public List<ContactMessage> listRecent(int limit) {
		int size = Math.max(0, limit);
		return contactMessageRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size));
	}

	@Transactional(readOnly = true)
	public long countUnread() {
		return contactMessageRepository.countByIsReadFalse();
	}
}
