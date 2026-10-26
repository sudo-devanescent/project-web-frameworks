package com.hostops.hotelmanagement.controllers.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostops.hotelmanagement.controllers.api.dto.CsrfDto;
import com.hostops.hotelmanagement.controllers.api.dto.LoginRequest;
import com.hostops.hotelmanagement.controllers.api.dto.SesionDto;
import com.hostops.hotelmanagement.security.SesionService;
import com.hostops.hotelmanagement.security.UsuarioAutenticado;
import com.hostops.hotelmanagement.services.exception.AutenticacionFallidaException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Expone el inicio y cierre de sesión, el usuario actual y el token CSRF.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SesionService sesionService;

	public AuthController(AuthenticationManager authenticationManager,
			SesionService sesionService) {
		this.authenticationManager = authenticationManager;
		this.sesionService = sesionService;
	}

	@GetMapping("/csrf")
	public CsrfDto csrf(CsrfToken token) {
		return new CsrfDto(token.getToken(), token.getHeaderName());
	}

	@PostMapping("/login")
	public SesionDto login(@Valid @RequestBody LoginRequest solicitud,
			HttpServletRequest request) {
		try {
			Authentication autenticacion = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							solicitud.nombreUsuario(), solicitud.contrasena()));
			SecurityContext contexto = SecurityContextHolder.createEmptyContext();
			contexto.setAuthentication(autenticacion);
			SecurityContextHolder.setContext(contexto);
			var sesionPrevia = request.getSession(false);
			if (sesionPrevia != null) {
				sesionService.eliminar(sesionPrevia.getId());
				sesionPrevia.invalidate();
			}
			HttpSession sesion = request.getSession(true);
			sesion.setAttribute(
					HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
					contexto);
			sesionService.registrar(autenticacion, sesion);
			return SesionDto.desde((UsuarioAutenticado) autenticacion.getPrincipal());
		} catch (AuthenticationException excepcion) {
			throw new AutenticacionFallidaException(
					"El usuario o la contraseña no son correctos.", excepcion);
		}
	}

	@GetMapping("/me")
	public SesionDto me(Authentication autenticacion) {
		return SesionDto.desde((UsuarioAutenticado) autenticacion.getPrincipal());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request,
			HttpServletResponse response, Authentication autenticacion) {
		HttpSession sesion = request.getSession(false);
		if (sesion != null) {
			sesionService.eliminar(sesion.getId());
		}
		new SecurityContextLogoutHandler().logout(request, response, autenticacion);
		return ResponseEntity.noContent().build();
	}

}
