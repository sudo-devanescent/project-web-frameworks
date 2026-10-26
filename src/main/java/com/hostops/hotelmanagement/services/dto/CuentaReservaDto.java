package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;

/**
 * Estado de cuenta resumido de una reserva.
 */
public record CuentaReservaDto(
		Integer idReserva,
		String titular,
		BigDecimal totalAlojamiento,
		BigDecimal totalPenalidades,
		BigDecimal totalAPagar,
		BigDecimal cobradoNeto,
		BigDecimal saldoPendiente,
		BigDecimal reembolsoPendiente) {
}
