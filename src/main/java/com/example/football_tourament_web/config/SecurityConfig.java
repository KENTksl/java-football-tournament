package com.example.football_tourament_web.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.football_tourament_web.model.enums.UserStatus;
import com.example.football_tourament_web.repository.AppUserRepository;

import jakarta.servlet.MultipartConfigElement;
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
						.disabled(u.getStatus() != UserStatus.ACTIVE)
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	@Bean
	public AuthenticationSuccessHandler authenticationSuccessHandler() {
		return (request, response, authentication) -> {
			boolean isAdmin = authentication.getAuthorities().stream()
					.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.sendRedirect(isAdmin ? "/admin" : "/ca-nhan");
		};
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
						"/user/tournament/**",
						"/dang-nhap",
						"/dang-nhap.html",
						"/dang-ky",
						"/dang-ky.html",
						"/api/geocode",
						"/thanh-toan/momo/**",
						"/order/momo-*"
				).permitAll()
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
				.failureUrl("/dang-nhap?error")
				.permitAll());

		http.logout(logout -> logout
				.logoutUrl("/dang-xuat")
				.logoutSuccessUrl("/dang-nhap?logout")
				.deleteCookies("JSESSIONID")
				.invalidateHttpSession(true)
				.permitAll());

		return http.build();
	}
}

