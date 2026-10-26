package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;

public record SaldoReservaDto(
		Integer idReserva,
		BigDecimal totalAlojamiento,
		BigDecimal totalPenalidades,
		BigDecimal totalAPagar,
		BigDecimal cobradoNeto,
		BigDecimal saldoPendiente,
		BigDecimal reembolsoPendiente) {
}
