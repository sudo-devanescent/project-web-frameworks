package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.MetodoPago;

public record CompensacionPagoComando(
		MetodoPago metodo,
		BigDecimal monto,
		Integer idUsuario) {

	public CompensacionPagoComando(MetodoPago metodo, BigDecimal monto) {
		this(metodo, monto, null);
	}
}
