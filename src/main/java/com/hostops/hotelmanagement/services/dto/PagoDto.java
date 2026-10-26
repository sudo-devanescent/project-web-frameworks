package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.hostops.hotelmanagement.models.MetodoPago;
import com.hostops.hotelmanagement.models.TipoMovimientoPago;

/**
 * Movimiento de pago asociado a una reserva.
 */
public record PagoDto(
		Integer idPago,
		Integer idReserva,
		Integer idUsuario,
		Integer idPagoOriginal,
		TipoMovimientoPago tipo,
		MetodoPago metodo,
		Instant fechaPago,
		BigDecimal monto,
		String motivo) {
}
