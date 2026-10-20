package com.hostops.hotelmanagement.models;

/**
 * Estados de habitación. Las constantes coinciden con las etiquetas del ENUM
 * nativo {@code estado_habitacion} de PostgreSQL (Hibernate escribe el name()).
 */
public enum EstadoHabitacion {
	disponible,
	ocupada,
	limpieza,
	mantenimiento
}
