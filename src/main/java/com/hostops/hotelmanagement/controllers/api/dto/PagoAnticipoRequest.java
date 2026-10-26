package com.hostops.hotelmanagement.controllers.api.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.MetodoPago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

/**
 * Anticipo opcional que se registra al crear una reserva.
 */
public record PagoAnticipoRequest(
		MetodoPago metodo,
		@DecimalMin(value = "0.00", message = "El anticipo no puede ser negativo.")
		@Digits(integer = 8, fraction = 2, message = "El anticipo admite como máximo 8 enteros y 2 decimales.")
		BigDecimal monto) {
}
