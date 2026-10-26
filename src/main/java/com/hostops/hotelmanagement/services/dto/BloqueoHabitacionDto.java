package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;

/**
 * Bloqueo de habitación con el número de habitación resuelto.
 */
public record BloqueoHabitacionDto(
		Integer idBloqueo,
		Integer idHabitacion,
		String numeroHabitacion,
		LocalDate fechaInicio,
		LocalDate fechaFin,
		String motivo) {
}
