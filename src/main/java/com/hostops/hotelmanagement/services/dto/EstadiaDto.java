package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;

import com.hostops.hotelmanagement.models.EstadoDetalle;

/**
 * Estancia en la que participa un huésped.
 */
public record EstadiaDto(
		Integer idReserva,
		Integer idHabitacion,
		String numeroHabitacion,
		LocalDate fechaEntradaPrevista,
		LocalDate fechaSalidaPrevista,
		EstadoDetalle estado) {
}
