package com.hostops.hotelmanagement.controllers.api.dto;

import com.hostops.hotelmanagement.models.EstadoHabitacion;

import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado operativo de una habitación.
 */
public record CambioEstadoHabitacionRequest(
		@NotNull(message = "El estado de la habitación es obligatorio.")
		EstadoHabitacion estado) {
}
