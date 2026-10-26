package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hostops.hotelmanagement.models.EstadoDetalle;

/**
 * Fila del listado de detalles de reserva.
 */
public record DetalleReservaListadoDto(
		Integer idDetalle,
		Integer idReserva,
		Integer idHabitacion,
		String numeroHabitacion,
		Integer idHuespedTitular,
		String titular,
		Integer idCanal,
		String canal,
		LocalDate fechaEntradaPrevista,
		LocalDate fechaSalidaPrevista,
		EstadoDetalle estado,
		BigDecimal saldoPendiente) {
}
