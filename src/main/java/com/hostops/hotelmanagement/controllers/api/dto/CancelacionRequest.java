package com.hostops.hotelmanagement.controllers.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos para cancelar un detalle de reserva.
 */
public record CancelacionRequest(
		@NotBlank(message = "El motivo de la cancelación es obligatorio.")
		String motivo,
		@DecimalMin(value = "0.00", message = "La penalidad no puede ser negativa.")
		@Digits(integer = 8, fraction = 2, message = "La penalidad admite como máximo 8 enteros y 2 decimales.")
		BigDecimal penalidad) {
}
