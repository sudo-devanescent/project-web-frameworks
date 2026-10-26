package com.hostops.hotelmanagement.controllers.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear una reserva completa de forma atómica.
 */
public record ReservaCrearRequest(
		@NotNull(message = "El huésped titular es obligatorio.")
		Integer idHuespedTitular,
		@NotNull(message = "El canal de la reserva es obligatorio.")
		Integer idCanal,
		@NotEmpty(message = "Debe indicarse al menos una habitación.")
		List<@NotNull(message = "La línea de reserva es obligatoria.") @Valid ReservaDetalleRequest> detalles,
		@Valid
		PagoAnticipoRequest pagoAnticipo) {
}
