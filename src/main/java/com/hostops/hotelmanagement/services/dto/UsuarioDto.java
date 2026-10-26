package com.hostops.hotelmanagement.services.dto;

import com.hostops.hotelmanagement.models.Usuario;

public record UsuarioDto(
		Integer idUsuario,
		Integer idRol,
		String nombreRol,
		String nombreUsuario,
		String nombreCompleto,
		String correo,
		Boolean activo) {

	public static UsuarioDto desde(Usuario usuario) {
		return new UsuarioDto(
				usuario.getIdUsuario(),
				usuario.getRol().getIdRol(),
				usuario.getRol().getNombre(),
				usuario.getNombreUsuario(),
				usuario.getNombreCompleto(),
				usuario.getCorreo(),
				usuario.getActivo());
	}

}
