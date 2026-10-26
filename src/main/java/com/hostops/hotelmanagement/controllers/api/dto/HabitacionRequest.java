package com.hostops.hotelmanagement.controllers.api.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.TipoHabitacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o modificar una habitación.
 */
public record HabitacionRequest(
		@NotBlank(message = "El número de habitación es obligatorio.")
		@Size(max = 10, message = "El número de habitación no puede superar 10 caracteres.")
		String numero,
		@NotNull(message = "El tipo de habitación es obligatorio.")
		TipoHabitacion tipo,
		@NotNull(message = "La capacidad es obligatoria.")
		@Positive(message = "La capacidad debe ser mayor que cero.")
		Short capacidad,
		@NotNull(message = "La tarifa es obligatoria.")
		@DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa.")
		@Digits(integer = 8, fraction = 2, message = "La tarifa admite como máximo 8 enteros y 2 decimales.")
		BigDecimal tarifa,
		EstadoHabitacion estado) {
}
