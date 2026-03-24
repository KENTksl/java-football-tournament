package com.example.football_tourament_web.service.user;

import com.example.football_tourament_web.service.core.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

@Service
public class UserAuthService {
	private final UserService userService;

	public UserAuthService(UserService userService) {
		this.userService = userService;
	}

	public String handleRegister(@Valid RegisterForm form, BindingResult bindingResult, Model model) {
		if (form.password != null && !form.password.equals(form.confirmPassword)) {
			bindingResult.rejectValue("confirmPassword", "confirmPassword.mismatch", "Mật khẩu nhập lại không khớp");
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("form", form);
			return "user/auth/register";
		}

		try {
			userService.registerUser(form.fullName, form.email, form.phone, form.password);
			return "redirect:/dang-nhap?registered";
		} catch (IllegalArgumentException ex) {
			bindingResult.rejectValue("email", "email.exists", ex.getMessage());
			model.addAttribute("form", form);
			return "user/auth/register";
		}
	}

	public static class RegisterForm {
		@NotBlank(message = "Vui lòng nhập họ tên")
		private String fullName;

		@NotBlank(message = "Vui lòng nhập email")
		@Email(message = "Email không hợp lệ")
		private String email;

		@NotBlank(message = "Vui lòng nhập số điện thoại")
		@Size(min = 9, max = 15, message = "Số điện thoại không hợp lệ")
		private String phone;

		@NotBlank(message = "Vui lòng nhập mật khẩu")
		@Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
		private String password;

		@NotBlank(message = "Vui lòng nhập lại mật khẩu")
		private String confirmPassword;

		@AssertTrue(message = "Vui lòng đồng ý với điều khoản")
		private boolean acceptTerms = true;

		public String getFullName() { return fullName; }
		public void setFullName(String fullName) { this.fullName = fullName; }
		public String getEmail() { return email; }
		public void setEmail(String email) { this.email = email; }
		public String getPhone() { return phone; }
		public void setPhone(String phone) { this.phone = phone; }
		public String getPassword() { return password; }
		public void setPassword(String password) { this.password = password; }
		public String getConfirmPassword() { return confirmPassword; }
		public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
		public boolean isAcceptTerms() { return acceptTerms; }
		public void setAcceptTerms(boolean acceptTerms) { this.acceptTerms = acceptTerms; }
	}
}
