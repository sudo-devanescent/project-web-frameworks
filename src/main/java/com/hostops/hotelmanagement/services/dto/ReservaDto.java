package com.hostops.hotelmanagement.services.dto;

import java.time.Instant;

/**
 * Cabecera de una reserva con los nombres de sus referencias.
 */
public record ReservaDto(
		Integer idReserva,
		Integer idHuespedTitular,
		String titular,
		Integer idCanal,
		String canal,
		Integer idUsuarioCreador,
		String creador,
		Integer idUsuarioModificador,
		String modificador,
		Instant fechaCreacion,
		Instant fechaModificacion) {
}
