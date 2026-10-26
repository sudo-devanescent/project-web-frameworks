package com.hostops.hotelmanagement.controllers.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para modificar una reserva y su detalle indicado.
 */
public record ReservaEditarRequest(
		@NotNull(message = "El huésped titular es obligatorio.")
		Integer idHuespedTitular,
		@NotNull(message = "El canal de la reserva es obligatorio.")
		Integer idCanal,
		@NotNull(message = "Los datos del detalle son obligatorios.")
		@Valid
		ReservaDetalleRequest detalle) {
}
