package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;

public record NochesOcupadasPorMesDto(
		LocalDate mes,
		long nochesOcupadas) {
}
