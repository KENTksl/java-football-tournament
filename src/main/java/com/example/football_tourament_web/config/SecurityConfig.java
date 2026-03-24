package com.example.football_tourament_web.config;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.example.football_tourament_web.model.enums.UserStatus;
import com.example.football_tourament_web.repository.AppUserRepository;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService(AppUserRepository userRepository) {
		return username -> userRepository.findByEmail(username)
				.map(u -> User.builder()
						.username(u.getEmail())
						.password(u.getPasswordHash() == null ? "" : u.getPasswordHash())
						.authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
						.disabled(false)
						.accountLocked(u.getStatus() == UserStatus.LOCKED)
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	@Bean
	public AuthenticationSuccessHandler authenticationSuccessHandler() {
		RequestCache requestCache = new HttpSessionRequestCache();
		return (request, response, authentication) -> {
			boolean isAdmin = authentication.getAuthorities().stream()
					.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
			String target = null;

			SavedRequest savedRequest = requestCache.getRequest(request, response);
			if (savedRequest != null) {
				target = safeRedirectTarget(savedRequest.getRedirectUrl(), request);
				requestCache.removeRequest(request, response);
			}

			if (target == null) {
				target = safeRedirectTarget(request.getParameter("redirect"), request);
			}

			if (target == null) {
				target = isAdmin ? "/admin" : "/ca-nhan";
			}
			if (isAdmin && !target.startsWith("/admin")) {
				target = "/admin";
			}
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.sendRedirect(target);
		};
	}

	private static String safeRedirectTarget(String raw, HttpServletRequest request) {
		if (raw == null) {
			return null;
		}
		String value = raw.trim();
		if (value.isBlank()) {
			return null;
		}
		if (value.contains("\r") || value.contains("\n") || value.contains("\\")) {
			return null;
		}

		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			if (scheme != null) {
				String host = uri.getHost();
				if (host == null || !host.equalsIgnoreCase(request.getServerName())) {
					return null;
				}
				int port = uri.getPort();
				if (port != -1 && port != request.getServerPort()) {
					return null;
				}
			}

			String path = uri.getRawPath();
			if (path == null || !path.startsWith("/") || path.startsWith("//")) {
				return null;
			}
			if (uri.getRawFragment() != null && !uri.getRawFragment().isBlank()) {
				return null;
			}
			String query = uri.getRawQuery();
			if (query == null || query.isBlank()) {
				return path;
			}
			return path + "?" + query;
		} catch (Exception ex) {
			if (!value.startsWith("/") || value.startsWith("//")) {
				return null;
			}
			return value;
		}
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler)
			throws Exception {
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/thanh-toan/momo/**", "/order/momo-*"));
		// Bỏ qua CSRF cho toàn bộ các API quản lý giải đấu để tránh lỗi 413 khi parse multipart
		http.csrf(csrf -> csrf
				.ignoringRequestMatchers("/admin/manage/tournament/**")
		);

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/user/tournament/sign-up", "/user/tournament/sign-up/**").hasAnyRole("USER", "ADMIN")
				.requestMatchers(
						"/assets/**",
						"/uploads/**",
						"/",
						"/home",
						"/gioi-thieu",
						"/gioi-thieu.html",
						"/lien-he",
						"/lien-he.html",
						"/tin-tuc",
						"/tin-tuc.html",
						"/dang-nhap",
						"/dang-nhap.html",
						"/dang-ky",
						"/dang-ky.html",
						"/api/geocode",
						"/thanh-toan/momo/**",
						"/order/momo-*"
				).permitAll()
				.requestMatchers("/user/tournament/**").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers(
						"/ca-nhan",
						"/ca-nhan.html",
						"/ca-nhan/**",
						"/thong-tin-doi",
						"/thong-tin-doi.html",
						"/thong-tin-doi/**",
						"/lich-su-dang-ky",
						"/lich-su-dang-ky.html",
						"/lich-su-giao-dich",
						"/lich-su-giao-dich.html",
						"/thanh-toan",
						"/thanh-toan.html"
				).hasAnyRole("USER", "ADMIN")
				.anyRequest().permitAll());

		http.formLogin(form -> form
				.loginPage("/dang-nhap")
				.loginProcessingUrl("/dang-nhap")
				.usernameParameter("email")
				.passwordParameter("password")
				.successHandler(successHandler)
				.failureHandler((request, response, exception) -> {
					String redirect = safeRedirectTarget(request.getParameter("redirect"), request);
					String suffix = "";
					if (redirect != null) {
						suffix = "&redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8);
					}
					if (exception instanceof LockedException) {
						response.sendRedirect("/dang-nhap?locked" + suffix);
						return;
					}
					response.sendRedirect("/dang-nhap?error" + suffix);
				})
				.permitAll());

		http.logout(logout -> logout
				.logoutUrl("/dang-xuat")
				.logoutSuccessUrl("/dang-nhap?logout")
				.deleteCookies("JSESSIONID")
				.invalidateHttpSession(true)
				.permitAll());

		http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}
}

