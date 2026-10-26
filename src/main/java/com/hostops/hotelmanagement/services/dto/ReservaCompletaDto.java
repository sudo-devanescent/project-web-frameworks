package com.hostops.hotelmanagement.services.dto;

import java.util.List;

/**
 * Reserva con todos sus detalles.
 */
public record ReservaCompletaDto(
		ReservaDto reserva,
		List<DetalleReservaDto> detalles) {
}
