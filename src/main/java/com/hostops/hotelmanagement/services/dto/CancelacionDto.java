package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cancelación registrada para un detalle de reserva.
 */
public record CancelacionDto(
		Integer idCancelacion,
		Integer idDetalle,
		Integer idUsuario,
		Instant fechaCancelacion,
		String motivo,
		BigDecimal penalidad) {
}
