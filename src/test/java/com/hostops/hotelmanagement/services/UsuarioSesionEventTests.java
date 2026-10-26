package com.hostops.hotelmanagement.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.security.SesionRevocadaEvent;
import com.hostops.hotelmanagement.services.dto.UsuarioComando;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class UsuarioSesionEventTests {

	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private ApplicationEvents events;

	@Test
	void unCambioDeContrasenaPublicaLaRevocacionDeSesiones() {
		Usuario usuario = usuarioRepository.findByNombreUsuario("rsantos").orElseThrow();
		String hashNuevo = "hash_nuevo_" + System.nanoTime();

		usuarioService.actualizar(usuario.getIdUsuario(), new UsuarioComando(
				usuario.getRol().getIdRol(), usuario.getNombreUsuario(),
				usuario.getNombreCompleto(), usuario.getCorreo(), hashNuevo,
				usuario.getActivo()));

		assertEquals(1, events.stream(SesionRevocadaEvent.class)
				.filter(evento -> evento.idUsuario().equals(usuario.getIdUsuario()))
				.count());
	}

	@Test
	void unaActualizacionSinCambiosSensiblesNoPublicaRevocacion() {
		Usuario usuario = usuarioRepository.findByNombreUsuario("rsantos").orElseThrow();

		usuarioService.actualizar(usuario.getIdUsuario(), new UsuarioComando(
				usuario.getRol().getIdRol(), usuario.getNombreUsuario(),
				usuario.getNombreCompleto(), usuario.getCorreo(), null,
				usuario.getActivo()));

		assertEquals(0, events.stream(SesionRevocadaEvent.class).count());
	}
}
