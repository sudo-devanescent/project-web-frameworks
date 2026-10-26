package com.hostops.hotelmanagement.controllers.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado activo de un usuario.
 */
public record UsuarioActivoRequest(
		@NotNull(message = "El estado activo es obligatorio.")
		Boolean activo) {
}
