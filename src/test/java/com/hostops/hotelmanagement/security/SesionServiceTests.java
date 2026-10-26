package com.hostops.hotelmanagement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SesionServiceTests {

	@AfterEach
	void limpiarContexto() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void revocaLasDemasSesionesDelUsuarioYConservaLaActual() {
		SessionRegistry registry = new SessionRegistryImpl();
		SesionService servicio = new SesionService(registry);
		UsuarioAutenticado principal = principal(7);
		Authentication autenticacion = new UsernamePasswordAuthenticationToken(
				principal, null, principal.getAuthorities());
		MockHttpSession actual = new MockHttpSession();
		MockHttpSession otra = new MockHttpSession();
		servicio.registrar(autenticacion, actual);
		servicio.registrar(autenticacion, otra);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(actual);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		servicio.revocar(7);

		assertFalse(registry.getSessionInformation(actual.getId()).isExpired());
		assertTrue(registry.getSessionInformation(otra.getId()).isExpired());
	}

	@Test
	void revocaTodasLasSesionesCuandoNoHayPeticionActual() {
		SessionRegistry registry = new SessionRegistryImpl();
		SesionService servicio = new SesionService(registry);
		UsuarioAutenticado principal = principal(9);
		Authentication autenticacion = new UsernamePasswordAuthenticationToken(
				principal, null, principal.getAuthorities());
		MockHttpSession sesion = new MockHttpSession();
		servicio.registrar(autenticacion, sesion);

		servicio.revocar(9);

		assertTrue(registry.getSessionInformation(sesion.getId()).isExpired());
	}

	@Test
	void dosInstanciasDelMismoUsuarioSonElMismoPrincipal() {
		UsuarioAutenticado uno = principal(3);
		UsuarioAutenticado otro = principal(3);

		assertEquals(uno, otro);
		assertEquals(uno.hashCode(), otro.hashCode());
		assertNotEquals(uno, principal(4));
	}

	private UsuarioAutenticado principal(Integer idUsuario) {
		return new UsuarioAutenticado(idUsuario, 2, "usuario" + idUsuario,
				"Usuario " + idUsuario, "recepcionista", "hash", true);
	}
}
