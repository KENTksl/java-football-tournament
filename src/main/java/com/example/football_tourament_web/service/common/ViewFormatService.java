package com.example.football_tourament_web.service.common;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ViewFormatService {
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private final NumberFormat moneyFormat;

	public ViewFormatService() {
		NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
		nf.setMaximumFractionDigits(0);
		nf.setMinimumFractionDigits(0);
		this.moneyFormat = nf;
	}

	public String formatDate(Instant instant) {
		if (instant == null) return null;
		return dateFormatter.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
	}

	public String formatDateTime(Instant instant) {
		if (instant == null) return null;
		return dateTimeFormatter.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
	}

	public String formatMoney(BigDecimal amount) {
		if (amount == null) return null;
		return moneyFormat.format(amount) + " đ";
	}
}

