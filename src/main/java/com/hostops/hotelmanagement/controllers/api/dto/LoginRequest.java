package com.hostops.hotelmanagement.controllers.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales para iniciar sesión en la API.
 */
public record LoginRequest(
		@NotBlank(message = "El nombre de usuario es obligatorio.")
		String nombreUsuario,
		@NotBlank(message = "La contraseña es obligatoria.")
		String contrasena) {
}
