package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;

import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.TipoHabitacion;

public record HabitacionComando(
		String numero,
		TipoHabitacion tipo,
		Short capacidad,
		BigDecimal tarifa,
		EstadoHabitacion estado) {
}
