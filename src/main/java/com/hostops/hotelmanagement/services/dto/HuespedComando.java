package com.hostops.hotelmanagement.services.dto;

import java.time.LocalDate;

public record HuespedComando(
		String tipoDocumento,
		String numeroDocumento,
		String nombres,
		String apellidos,
		String telefono,
		String correo,
		String nacionalidad,
		LocalDate fechaNacimiento,
		String preferencias) {
}
