package com.hostops.hotelmanagement.controllers.api.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.MetodoPago;
import com.hostops.hotelmanagement.models.TipoMovimientoPago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un movimiento de pago.
 */
public record PagoRequest(
		@NotNull(message = "El tipo de movimiento es obligatorio.")
		TipoMovimientoPago tipo,
		@NotNull(message = "El método de pago es obligatorio.")
		MetodoPago metodo,
		@NotNull(message = "El monto es obligatorio.")
		@DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero.")
		@Digits(integer = 8, fraction = 2, message = "El monto admite como máximo 8 enteros y 2 decimales.")
		BigDecimal monto,
		@Size(max = 500, message = "El motivo no puede superar 500 caracteres.")
		String motivo) {
}
