package com.hostops.hotelmanagement.controllers.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear o modificar un bloqueo de habitación.
 */
public record BloqueoHabitacionRequest(
		@NotNull(message = "La fecha de inicio del bloqueo es obligatoria.")
		LocalDate fechaInicio,
		@NotNull(message = "La fecha de fin del bloqueo es obligatoria.")
		LocalDate fechaFin,
		@NotBlank(message = "El motivo del bloqueo es obligatorio.")
		String motivo) {
}
