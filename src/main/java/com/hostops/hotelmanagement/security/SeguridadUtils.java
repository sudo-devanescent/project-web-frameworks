package com.hostops.hotelmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.hostops.hotelmanagement.services.exception.AutenticacionFallidaException;

import org.springframework.security.access.AccessDeniedException;

/**
 * Utilidades para obtener el usuario autenticado desde los controladores.
 */
public final class SeguridadUtils {

	private SeguridadUtils() {
	}

	public static UsuarioAutenticado usuarioActual() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null
				|| !(autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario)) {
			throw new AutenticacionFallidaException("Debes iniciar sesión para realizar esta operación.");
		}
		return usuario;
	}

	public static Integer idUsuarioActual() {
		return usuarioActual().getIdUsuario();
	}

	public static boolean esAdministrador() {
		return "administrador".equalsIgnoreCase(usuarioActual().getRol());
	}

	public static void exigirAdministrador() {
		if (!esAdministrador()) {
			throw new AccessDeniedException(
					"Solo un administrador puede realizar esta operación.");
		}
	}

}
