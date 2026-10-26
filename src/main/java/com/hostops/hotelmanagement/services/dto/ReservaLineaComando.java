package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Línea de habitación para crear una reserva completa.
 */
public record ReservaLineaComando(
		Integer idHabitacion,
		LocalDate fechaEntradaPrevista,
		LocalDate fechaSalidaPrevista,
		List<Integer> idHuespedes) {
}
