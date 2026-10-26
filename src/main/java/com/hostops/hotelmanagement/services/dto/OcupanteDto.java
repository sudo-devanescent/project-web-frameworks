package com.hostops.hotelmanagement.services.dto;

/**
 * Huésped alojado en un detalle de reserva.
 */
public record OcupanteDto(
		Integer idHuesped,
		String nombreCompleto) {
}
