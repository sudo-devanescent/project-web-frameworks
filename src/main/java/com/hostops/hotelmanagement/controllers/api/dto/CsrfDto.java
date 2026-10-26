package com.hostops.hotelmanagement.controllers.api.dto;

/**
 * Token CSRF y cabecera que debe enviar el cliente.
 */
public record CsrfDto(
		String token,
		String headerName) {
}
