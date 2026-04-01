package com.example.football_tourament_web.controller.common;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class StaticImageController {
	@GetMapping("/favicon.ico")
	@ResponseBody
	public ResponseEntity<Resource> favicon() {
		Resource resource = new ClassPathResource("static/assets/logo/logo-signature.png");
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.header("Cache-Control", "no-cache, no-store, must-revalidate")
				.header("Pragma", "no-cache")
				.header("Expires", "0")
				.body(resource);
	}

	@GetMapping(value = "/img/figma-avatar.png", produces = MediaType.IMAGE_JPEG_VALUE)
	@ResponseBody
	public ResponseEntity<Resource> defaultAvatar() {
		Resource resource = new ClassPathResource("static/assets/figma/avatar.jpg");
		return ResponseEntity.ok(resource);
	}

	@GetMapping(value = "/img/home-hero.jpg", produces = MediaType.IMAGE_PNG_VALUE)
	@ResponseBody
	public ResponseEntity<Resource> defaultHomeHero() {
		Resource resource = new ClassPathResource("static/assets/figma/home-hero.png");
		return ResponseEntity.ok(resource);
	}
}

