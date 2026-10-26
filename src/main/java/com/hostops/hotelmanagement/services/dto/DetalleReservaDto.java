package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.TipoHabitacion;

/**
 * Detalle completo de una habitación reservada.
 */
public record DetalleReservaDto(
		Integer idDetalle,
		ReservaDto reserva,
		Integer idHabitacion,
		String numeroHabitacion,
		TipoHabitacion tipoHabitacion,
		Short capacidadHabitacion,
		EstadoHabitacion estadoHabitacion,
		LocalDate fechaEntradaPrevista,
		LocalDate fechaSalidaPrevista,
		Instant checkinReal,
		Instant checkoutReal,
		BigDecimal tarifaNoche,
		EstadoDetalle estado,
		BigDecimal saldoPendiente,
		List<OcupanteDto> huespedes) {
}
