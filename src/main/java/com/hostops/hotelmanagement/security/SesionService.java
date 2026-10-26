package com.hostops.hotelmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

/**
 * Registra las sesiones autenticadas y permite revocarlas por usuario.
 */
@Service
public class SesionService {

	private final SessionRegistry sessionRegistry;

	public SesionService(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	public void registrar(Authentication autenticacion, HttpSession sesion) {
		if (autenticacion == null || sesion == null) {
			return;
		}
		sessionRegistry.registerNewSession(sesion.getId(), autenticacion.getPrincipal());
	}

	public void eliminar(String idSesion) {
		if (idSesion != null) {
			sessionRegistry.removeSessionInformation(idSesion);
		}
	}

	public void revocar(Integer idUsuario) {
		if (idUsuario == null) {
			return;
		}
		String idSesionActual = idSesionActual();
		for (Object principal : sessionRegistry.getAllPrincipals()) {
			if (!(principal instanceof UserDetails usuario)) {
				continue;
			}
			Integer idPrincipal = principal instanceof UsuarioAutenticado autenticado
					? autenticado.getIdUsuario()
					: null;
			if (idPrincipal == null || !idUsuario.equals(idPrincipal)) {
				continue;
			}
			for (SessionInformation sesion : sessionRegistry.getAllSessions(principal, false)) {
				if (!sesion.getSessionId().equals(idSesionActual)) {
					sesion.expireNow();
				}
			}
		}
	}

	private String idSesionActual() {
		RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
		if (atributos instanceof ServletRequestAttributes servlet) {
			HttpSession sesion = servlet.getRequest().getSession(false);
			return sesion == null ? null : sesion.getId();
		}
		return null;
	}
}
