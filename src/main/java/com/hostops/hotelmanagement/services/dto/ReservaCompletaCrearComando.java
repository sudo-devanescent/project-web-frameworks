package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;
import java.util.List;

import com.hostops.hotelmanagement.models.MetodoPago;

/**
 * Datos para crear una reserva completa y su anticipo opcional.
 */
public record ReservaCompletaCrearComando(
		Integer idHuespedTitular,
		Integer idCanal,
		Integer idUsuarioCreador,
		List<ReservaLineaComando> detalles,
		MetodoPago metodoAnticipo,
		BigDecimal montoAnticipo) {
}
