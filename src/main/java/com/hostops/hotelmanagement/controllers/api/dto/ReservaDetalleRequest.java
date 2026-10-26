package com.hostops.hotelmanagement.controllers.api.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Habitación, fechas y ocupantes de una línea de reserva.
 */
public record ReservaDetalleRequest(
		@NotNull(message = "La habitación es obligatoria.")
		Integer idHabitacion,
		@NotNull(message = "La fecha de entrada es obligatoria.")
		LocalDate fechaEntradaPrevista,
		@NotNull(message = "La fecha de salida es obligatoria.")
		LocalDate fechaSalidaPrevista,
		@NotEmpty(message = "Debe indicarse al menos un ocupante.")
		List<Integer> huespedes) {
}
