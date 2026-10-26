package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen financiero con cuentas por reserva y movimientos recientes.
 */
public record FinanzasResumenDto(
		BigDecimal cobradoNetoMes,
		BigDecimal saldosPorCobrar,
		BigDecimal reembolsosPorDevolver,
		List<CuentaReservaDto> cuentas,
		List<PagoDto> movimientos) {
}
