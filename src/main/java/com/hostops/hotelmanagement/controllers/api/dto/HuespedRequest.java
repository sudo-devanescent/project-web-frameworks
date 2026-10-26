package com.hostops.hotelmanagement.controllers.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o modificar un huésped.
 */
public record HuespedRequest(
		@NotBlank(message = "El tipo de documento es obligatorio.")
		@Size(max = 20, message = "El tipo de documento no puede superar 20 caracteres.")
		String tipoDocumento,
		@NotBlank(message = "El número de documento es obligatorio.")
		@Size(max = 30, message = "El número de documento no puede superar 30 caracteres.")
		String numeroDocumento,
		@NotBlank(message = "Los nombres son obligatorios.")
		@Size(max = 100, message = "Los nombres no pueden superar 100 caracteres.")
		String nombres,
		@NotBlank(message = "Los apellidos son obligatorios.")
		@Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres.")
		String apellidos,
		@NotBlank(message = "El teléfono es obligatorio.")
		@Size(max = 30, message = "El teléfono no puede superar 30 caracteres.")
		String telefono,
		@Email(message = "El correo no tiene un formato válido.")
		@Size(max = 120, message = "El correo no puede superar 120 caracteres.")
		String correo,
		@NotBlank(message = "La nacionalidad es obligatoria.")
		@Size(max = 60, message = "La nacionalidad no puede superar 60 caracteres.")
		String nacionalidad,
		@NotNull(message = "La fecha de nacimiento es obligatoria.")
		@PastOrPresent(message = "La fecha de nacimiento no puede ser futura.")
		LocalDate fechaNacimiento,
		String preferencias) {
}
