package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.MetodoPago;
import com.hostops.hotelmanagement.models.TipoMovimientoPago;

public record PagoComando(
		TipoMovimientoPago tipo,
		MetodoPago metodo,
		BigDecimal monto,
		Integer idUsuario,
		String motivo) {

	public PagoComando(TipoMovimientoPago tipo, MetodoPago metodo, BigDecimal monto) {
		this(tipo, metodo, monto, null, null);
	}
}
