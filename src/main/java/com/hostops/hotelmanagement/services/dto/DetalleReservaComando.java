package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DetalleReservaComando(
		Integer idHabitacion,
		LocalDate fechaEntradaPrevista,
		LocalDate fechaSalidaPrevista,
		BigDecimal tarifaNoche) {
}
