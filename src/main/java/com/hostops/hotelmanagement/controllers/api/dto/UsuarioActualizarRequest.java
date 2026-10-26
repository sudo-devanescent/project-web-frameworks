package com.hostops.hotelmanagement.controllers.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para modificar un usuario; la contraseña es opcional.
 */
public record UsuarioActualizarRequest(
		@NotNull(message = "El rol del usuario es obligatorio.")
		Integer idRol,
		@NotBlank(message = "El nombre de usuario es obligatorio.")
		@Size(max = 50, message = "El nombre de usuario no puede superar 50 caracteres.")
		String nombreUsuario,
		@NotBlank(message = "El nombre completo es obligatorio.")
		@Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
		String nombreCompleto,
		@NotBlank(message = "El correo es obligatorio.")
		@Email(message = "El correo no tiene un formato válido.")
		@Size(max = 120, message = "El correo no puede superar 120 caracteres.")
		String correo,
		@Size(max = 72, message = "La contraseña no puede superar 72 caracteres.")
		String contrasena,
		Boolean activo) {
}
