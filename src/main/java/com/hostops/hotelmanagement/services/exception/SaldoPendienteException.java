package com.hostops.hotelmanagement.services.exception;

import java.math.BigDecimal;

/**
 * Indica que una reserva con saldo pendiente no puede completar la operación.
 */
public class SaldoPendienteException extends ReglaNegocioException {

	private final BigDecimal saldo;

	public SaldoPendienteException(String mensaje, BigDecimal saldo) {
		super(mensaje);
		this.saldo = saldo;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

}
