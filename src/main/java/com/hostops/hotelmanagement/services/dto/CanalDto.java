package com.hostops.hotelmanagement.services.dto;

/**
 * Canal de reserva y número de reservas asociadas.
 */
public record CanalDto(
		Integer idCanal,
		String nombre,
		long reservas) {
}
