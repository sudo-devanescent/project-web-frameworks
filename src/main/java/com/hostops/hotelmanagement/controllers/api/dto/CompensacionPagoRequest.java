package com.hostops.hotelmanagement.controllers.api.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.MetodoPago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

/**
 * Datos para compensar un pago o reembolso existente.
 */
public record CompensacionPagoRequest(
		MetodoPago metodo,
		@DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero.")
		@Digits(integer = 8, fraction = 2, message = "El monto admite como máximo 8 enteros y 2 decimales.")
		BigDecimal monto) {
}
