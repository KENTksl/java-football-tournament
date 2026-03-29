package com.example.football_tourament_web.service.admin;

import com.example.football_tourament_web.model.entity.Tournament;
import com.example.football_tourament_web.model.enums.PitchType;
import com.example.football_tourament_web.model.enums.TournamentMode;
import com.example.football_tourament_web.model.enums.TournamentStatus;
import com.example.football_tourament_web.service.common.FileStorageService;
import com.example.football_tourament_web.service.core.TournamentRegistrationService;
import com.example.football_tourament_web.service.core.TournamentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminTournamentManagementService {
	private final TournamentService tournamentService;
	private final TournamentRegistrationService tournamentRegistrationService;
	private final FileStorageService fileStorageService;

	public AdminTournamentManagementService(
			TournamentService tournamentService,
			TournamentRegistrationService tournamentRegistrationService,
			FileStorageService fileStorageService
	) {
		this.tournamentService = tournamentService;
		this.tournamentRegistrationService = tournamentRegistrationService;
		this.fileStorageService = fileStorageService;
	}

	@Transactional(readOnly = true)
	public List<Tournament> listTournaments() {
		return tournamentService.listTournaments();
	}

	@Transactional(readOnly = true)
	public Tournament findById(Long id) {
		if (id == null) return null;
		return tournamentService.findById(id).orElse(null);
	}

	@Transactional(readOnly = true)
	public Map<Long, Long> buildRegisteredTeamCountMap(List<Tournament> tournaments) {
		Map<Long, Long> map = new HashMap<>();
		if (tournaments == null) return map;
		for (Tournament t : tournaments) {
			if (t == null || t.getId() == null) continue;
			map.put(t.getId(), tournamentRegistrationService.countRegisteredTeams(t.getId()));
		}
		return map;
	}

	@Transactional(readOnly = true)
	public List<AdminTournamentRow> buildTournamentRows(List<Tournament> tournaments, Map<Long, Long> registeredTeamCounts) {
		if (tournaments == null) return List.of();
		return tournaments.stream()
				.filter(t -> t != null && t.getId() != null)
				.map(t -> {
					long registered = registeredTeamCounts == null ? 0L : registeredTeamCounts.getOrDefault(t.getId(), 0L);
					int limit = t.getTeamLimit() == null ? 0 : t.getTeamLimit();
					String teamCountText = registered + "/" + limit;
					String modeLabel = t.getMode() == TournamentMode.GROUP_STAGE ? "Chia bảng đấu (Group Stage)" : "Knockout";
					TournamentStatus s = t.getStatus();
					String statusLabel = s == TournamentStatus.UPCOMING ? "Sắp diễn ra" : (s == TournamentStatus.LIVE ? "Đang đá" : "Đã kết thúc");
					String statusClass = s == TournamentStatus.UPCOMING ? "status-badge--upcoming" : (s == TournamentStatus.LIVE ? "status-badge--live" : "status-badge--finished");
					String feeText = t.getRegistrationFee() == null ? "0" : t.getRegistrationFee().stripTrailingZeros().toPlainString();
					return new AdminTournamentRow(
							t.getId(),
							t.getName(),
							t.getOrganizer(),
							modeLabel,
							teamCountText,
							statusLabel,
							statusClass,
							t.getMode() == null ? null : t.getMode().name(),
							t.getPitchType() == null ? null : t.getPitchType().name(),
							t.getTeamLimit(),
							feeText,
							t.getStartDate() == null ? null : t.getStartDate().toString(),
							t.getEndDate() == null ? null : t.getEndDate().toString(),
							t.getDescription()
					);
				})
				.toList();
	}

	@Transactional
	public void addTournament(
			String name,
			String organizer,
			String mode,
			String pitchType,
			String teams,
			String registrationFee,
			String startDate,
			String endDate,
			String description,
			MultipartFile image
	) {
		Tournament tournament = new Tournament();
		applyTournamentFromInputs(tournament, name, organizer, mode, pitchType, teams, registrationFee, startDate, endDate, description, image);
		tournamentService.save(tournament);
	}

	@Transactional
	public void editTournament(
			Long id,
			String name,
			String organizer,
			String mode,
			String pitchType,
			String teams,
			String registrationFee,
			String startDate,
			String endDate,
			String description,
			MultipartFile image
	) {
		if (id == null) return;
		Tournament tournament = tournamentService.findById(id).orElse(null);
		if (tournament == null) return;
		applyTournamentFromInputs(tournament, name, organizer, mode, pitchType, teams, registrationFee, startDate, endDate, description, image);
		tournamentService.save(tournament);
	}

	@Transactional
	public void deleteTournament(Long id) {
		if (id == null) return;
		tournamentService.deleteById(id);
	}

	private void applyTournamentFromInputs(
			Tournament tournament,
			String name,
			String organizer,
			String mode,
			String pitchType,
			String teams,
			String registrationFee,
			String startDate,
			String endDate,
			String description,
			MultipartFile image
	) {
		tournament.setName(name);
		tournament.setOrganizer(organizer);

		if (mode != null && mode.contains("Group")) {
			tournament.setMode(TournamentMode.GROUP_STAGE);
		} else {
			tournament.setMode(TournamentMode.KNOCKOUT);
		}

		if (pitchType != null && pitchType.contains("5")) {
			tournament.setPitchType(PitchType.PITCH_5);
		} else if (pitchType != null && pitchType.contains("11")) {
			tournament.setPitchType(PitchType.PITCH_11);
		} else {
			tournament.setPitchType(PitchType.PITCH_7);
		}

		try {
			String teamCount = teams == null ? "" : teams.split("/")[0];
			tournament.setTeamLimit(Integer.parseInt(teamCount));
		} catch (Exception ignored) {
			if (tournament.getTeamLimit() == null) tournament.setTeamLimit(4);
		}

		tournament.setRegistrationFee(parseMoneyOrZero(registrationFee));

		try {
			if (startDate != null && !startDate.isBlank()) {
				tournament.setStartDate(LocalDate.parse(startDate));
			}
			if (endDate != null && !endDate.isBlank()) {
				tournament.setEndDate(LocalDate.parse(endDate));
			}
		} catch (Exception ignored) {
			if (tournament.getStartDate() == null) tournament.setStartDate(LocalDate.now());
			if (tournament.getEndDate() == null) tournament.setEndDate(LocalDate.now().plusMonths(1));
		}

		tournament.setDescription(description);
		tournament.setStatus(calculateStatus(tournament.getStartDate(), tournament.getEndDate()));

		if (image != null && !image.isEmpty()) {
			String url = fileStorageService.storeUnderUploads(image, "tournaments");
			if (url != null) tournament.setImageUrl(url);
		}
	}

	private static BigDecimal parseMoneyOrZero(String raw) {
		if (raw == null) return BigDecimal.ZERO;
		String s = raw.trim();
		if (s.isBlank()) return BigDecimal.ZERO;
		s = s.replaceAll("[^0-9]", "");
		if (s.isBlank()) return BigDecimal.ZERO;
		try {
			return new BigDecimal(s);
		} catch (Exception ex) {
			return BigDecimal.ZERO;
		}
	}

	private TournamentStatus calculateStatus(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null) return TournamentStatus.UPCOMING;
		LocalDate today = LocalDate.now();
		if (today.isBefore(startDate)) return TournamentStatus.UPCOMING;
		if (today.isAfter(endDate)) return TournamentStatus.FINISHED;
		return TournamentStatus.LIVE;
	}

	public static final class AdminTournamentRow {
		private final Long id;
		private final String name;
		private final String organizer;
		private final String modeLabel;
		private final String teamCountText;
		private final String statusLabel;
		private final String statusClass;
		private final String mode;
		private final String pitchType;
		private final Integer teamLimit;
		private final String registrationFee;
		private final String startDate;
		private final String endDate;
		private final String description;

		public AdminTournamentRow(Long id, String name, String organizer, String modeLabel, String teamCountText, String statusLabel, String statusClass, String mode, String pitchType, Integer teamLimit, String registrationFee, String startDate, String endDate, String description) {
			this.id = id;
			this.name = name;
			this.organizer = organizer;
			this.modeLabel = modeLabel;
			this.teamCountText = teamCountText;
			this.statusLabel = statusLabel;
			this.statusClass = statusClass;
			this.mode = mode;
			this.pitchType = pitchType;
			this.teamLimit = teamLimit;
			this.registrationFee = registrationFee;
			this.startDate = startDate;
			this.endDate = endDate;
			this.description = description;
		}

		public Long getId() { return id; }
		public String getName() { return name; }
		public String getOrganizer() { return organizer; }
		public String getModeLabel() { return modeLabel; }
		public String getTeamCountText() { return teamCountText; }
		public String getStatusLabel() { return statusLabel; }
		public String getStatusClass() { return statusClass; }
		public String getMode() { return mode; }
		public String getPitchType() { return pitchType; }
		public Integer getTeamLimit() { return teamLimit; }
		public String getRegistrationFee() { return registrationFee; }
		public String getStartDate() { return startDate; }
		public String getEndDate() { return endDate; }
		public String getDescription() { return description; }
	}
}
