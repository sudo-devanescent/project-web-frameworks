package com.hostops.hotelmanagement.controllers.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o modificar un canal de reserva.
 */
public record CanalRequest(
		@NotBlank(message = "El nombre del canal es obligatorio.")
		@Size(max = 50, message = "El nombre del canal no puede superar 50 caracteres.")
		String nombre) {
}
