package com.hostops.hotelmanagement.services.dto;

/**
 * Rol disponible y número de usuarios asignados.
 */
public record RolDto(
		Integer idRol,
		String nombre,
		long usuarios) {
}
