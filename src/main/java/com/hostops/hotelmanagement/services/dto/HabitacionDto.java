package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.Habitacion;
import com.hostops.hotelmanagement.models.TipoHabitacion;

/**
 * Datos públicos de una habitación.
 */
public record HabitacionDto(
		Integer idHabitacion,
		String numero,
		TipoHabitacion tipo,
		Short capacidad,
		BigDecimal tarifa,
		EstadoHabitacion estado) {

	public static HabitacionDto desde(Habitacion habitacion) {
		return new HabitacionDto(
				habitacion.getIdHabitacion(),
				habitacion.getNumero(),
				habitacion.getTipo(),
				habitacion.getCapacidad(),
				habitacion.getTarifa(),
				habitacion.getEstado());
	}

}
