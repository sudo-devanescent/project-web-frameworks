package com.hostops.hotelmanagement.services.dto;

public record UsuarioComando(
		Integer idRol,
		String nombreUsuario,
		String nombreCompleto,
		String correo,
		String contrasenaHash,
		Boolean activo) {
}
