package com.example.football_tourament_web.controller.user;

import com.example.football_tourament_web.service.user.UserTeamService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class UserTeamController {
	private final UserTeamService userTeamService;

	public UserTeamController(UserTeamService userTeamService) {
		this.userTeamService = userTeamService;
	}

	@GetMapping({"/thong-tin-doi", "/thong-tin-doi.html"})
	public String teamInfo(
			@RequestParam(name = "teamId", required = false) Long teamId,
			@RequestParam(name = "tournamentId", required = false) Long tournamentId,
			@RequestParam(name = "tab", required = false) String tab,
			@RequestParam(name = "create", required = false) String create,
			@RequestParam(name = "edit", required = false) String edit,
			Model model,
			Authentication authentication
	) {
		return userTeamService.buildTeamInfoPage(teamId, tournamentId, tab, create, edit, model, authentication);
	}

	@PostMapping("/thong-tin-doi/tao-doi")
	public String createTeam(
			@Valid UserTeamService.TeamCreateForm teamForm,
			BindingResult bindingResult,
			@RequestParam(name = "teamLogo", required = false) MultipartFile teamLogo,
			@RequestParam(name = "memberName", required = false) List<String> memberNames,
			@RequestParam(name = "memberJersey", required = false) List<Integer> memberJerseys,
			@RequestParam(name = "memberAvatar", required = false) MultipartFile[] memberAvatars,
			Authentication authentication,
			Model model
	) {
		return userTeamService.createTeam(teamForm, bindingResult, teamLogo, memberNames, memberJerseys, memberAvatars, authentication, model);
	}

	@PostMapping("/thong-tin-doi/cap-nhat-doi")
	public String updateTeam(
			@RequestParam("teamId") Long teamId,
			@Valid UserTeamService.TeamCreateForm teamForm,
			BindingResult bindingResult,
			@RequestParam(name = "existingLogoUrl", required = false) String existingLogoUrl,
			@RequestParam(name = "teamLogo", required = false) MultipartFile teamLogo,
			@RequestParam(name = "memberName", required = false) List<String> memberNames,
			@RequestParam(name = "memberJersey", required = false) List<Integer> memberJerseys,
			@RequestParam(name = "existingMemberAvatar", required = false) List<String> existingMemberAvatars,
			@RequestParam(name = "memberAvatar", required = false) MultipartFile[] memberAvatars,
			Authentication authentication,
			Model model
	) {
		return userTeamService.updateTeam(teamId, teamForm, bindingResult, existingLogoUrl, teamLogo, memberNames, memberJerseys, existingMemberAvatars, memberAvatars, authentication, model);
	}
}

