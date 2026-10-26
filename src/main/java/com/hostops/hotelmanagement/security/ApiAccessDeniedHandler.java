package com.hostops.hotelmanagement.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Devuelve un error JSON cuando el usuario no tiene permisos suficientes.
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"No tienes permisos para realizar esta operación.");
		problema.setTitle("Acceso denegado");
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problema);
	}

}
