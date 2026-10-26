package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;

/**
 * Datos completos de un huésped y número de estadías no canceladas.
 */
public record HuespedDto(
		Integer idHuesped,
		String tipoDocumento,
		String numeroDocumento,
		String nombres,
		String apellidos,
		String telefono,
		String correo,
		String nacionalidad,
		LocalDate fechaNacimiento,
		String preferencias,
		long estadias) {
}
