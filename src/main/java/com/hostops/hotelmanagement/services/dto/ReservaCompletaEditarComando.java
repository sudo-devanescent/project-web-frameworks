package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos para modificar la cabecera y un detalle de una reserva.
 */
public record ReservaCompletaEditarComando(
		Integer idHuespedTitular,
		Integer idCanal,
		Integer idUsuarioModificador,
		Integer idHabitacion,
		LocalDate fechaEntradaPrevista,
		LocalDate fechaSalidaPrevista,
		List<Integer> idHuespedes) {
}
