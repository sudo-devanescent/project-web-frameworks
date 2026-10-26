package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicadores y listados operativos del panel diario.
 */
public record DashboardResumenDto(
		long totalHabitaciones,
		long habitacionesOcupadas,
		long habitacionesDisponibles,
		long habitacionesLimpieza,
		long habitacionesMantenimiento,
		long reservasActivas,
		BigDecimal ingresosNetosMes,
		BigDecimal saldosPendientes,
		List<DetalleReservaListadoDto> llegadasPendientes,
		List<DetalleReservaListadoDto> salidasPendientes,
		List<CuentaReservaDto> cuentasPendientes) {
}
