package com.hostops.hotelmanagement.controllers.api.dto;

import com.hostops.hotelmanagement.security.UsuarioAutenticado;

/**
 * Datos del usuario con sesión activa.
 */
public record SesionDto(
		Integer idUsuario,
		Integer idRol,
		String nombreUsuario,
		String nombreCompleto,
		String rol) {

	public static SesionDto desde(UsuarioAutenticado usuario) {
		return new SesionDto(
				usuario.getIdUsuario(),
				usuario.getIdRol(),
				usuario.getUsername(),
				usuario.getNombreCompleto(),
				usuario.getRol());
	}

}
