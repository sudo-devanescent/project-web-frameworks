package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;

public record BloqueoHabitacionComando(
		Integer idHabitacion,
		LocalDate fechaInicio,
		LocalDate fechaFin,
		String motivo) {
}
