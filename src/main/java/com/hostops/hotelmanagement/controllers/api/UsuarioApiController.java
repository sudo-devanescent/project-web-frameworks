package com.hostops.hotelmanagement.controllers.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostops.hotelmanagement.controllers.api.dto.UsuarioActivoRequest;
import com.hostops.hotelmanagement.controllers.api.dto.UsuarioActualizarRequest;
import com.hostops.hotelmanagement.controllers.api.dto.UsuarioCrearRequest;
import com.hostops.hotelmanagement.services.UsuarioService;
import com.hostops.hotelmanagement.services.dto.UsuarioComando;
import com.hostops.hotelmanagement.services.dto.UsuarioDto;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;

import jakarta.validation.Valid;

/**
 * Expone la consulta y administración de usuarios.
 */
@RestController
@RequestMapping("/api/usuarios")
@Validated
public class UsuarioApiController {

	private static final int LONGITUD_MINIMA_CONTRASENA = 8;
	private static final int LONGITUD_MAXIMA_HASH_BYTES = 72;

	private final UsuarioService usuarioService;
	private final PasswordEncoder passwordEncoder;

	public UsuarioApiController(UsuarioService usuarioService,
			PasswordEncoder passwordEncoder) {
		this.usuarioService = usuarioService;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public List<UsuarioDto> listar() {
		return usuarioService.listar();
	}

	@GetMapping("/{id}")
	public UsuarioDto obtener(@PathVariable Integer id) {
		return usuarioService.obtener(id);
	}

	@PostMapping
	public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody UsuarioCrearRequest solicitud) {
		String hash = hashContrasena(solicitud.contrasena(), true);
		UsuarioDto creado = usuarioService.crear(new UsuarioComando(
				solicitud.idRol(),
				solicitud.nombreUsuario(),
				solicitud.nombreCompleto(),
				solicitud.correo(),
				hash,
				solicitud.activo()));
		return ResponseEntity.created(URI.create("/api/usuarios/" + creado.idUsuario()))
				.body(creado);
	}

	@PutMapping("/{id}")
	public UsuarioDto actualizar(@PathVariable Integer id,
			@Valid @RequestBody UsuarioActualizarRequest solicitud) {
		String hash = hashContrasena(solicitud.contrasena(), false);
		return usuarioService.actualizar(id, new UsuarioComando(
				solicitud.idRol(),
				solicitud.nombreUsuario(),
				solicitud.nombreCompleto(),
				solicitud.correo(),
				hash,
				solicitud.activo()));
	}

	@PatchMapping("/{id}/activo")
	public UsuarioDto cambiarActivo(@PathVariable Integer id,
			@Valid @RequestBody UsuarioActivoRequest solicitud) {
		return usuarioService.cambiarActivo(id, solicitud.activo());
	}

	private String hashContrasena(String contrasena, boolean obligatoria) {
		if (contrasena == null || contrasena.isBlank()) {
			if (obligatoria) {
				throw new DatosInvalidosException("La contraseña es obligatoria.");
			}
			return null;
		}
		if (contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
			throw new DatosInvalidosException(
					"La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA
							+ " caracteres.");
		}
		if (contrasena.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_HASH_BYTES) {
			throw new DatosInvalidosException(
					"La contraseña no puede superar 72 bytes en UTF-8.");
		}
		return passwordEncoder.encode(contrasena);
	}

}
