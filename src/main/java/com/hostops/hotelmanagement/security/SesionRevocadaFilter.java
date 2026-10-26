package com.hostops.hotelmanagement.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Invalida la petición cuando la sesión ha sido revocada por un administrador.
 */
public class SesionRevocadaFilter extends OncePerRequestFilter {

	private final SessionRegistry sessionRegistry;

	public SesionRevocadaFilter(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		HttpSession sesion = request.getSession(false);
		if (sesion != null) {
			SessionInformation informacion = sessionRegistry.getSessionInformation(sesion.getId());
			if (informacion != null && informacion.isExpired()) {
				Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
				new SecurityContextLogoutHandler().logout(request, response, autenticacion);
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
