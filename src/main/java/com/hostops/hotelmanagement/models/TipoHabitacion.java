package com.hostops.hotelmanagement.models;

/**
 * Tipos de habitación. Las constantes coinciden con las etiquetas del ENUM
 * nativo {@code tipo_habitacion} de PostgreSQL (Hibernate escribe el name()).
 */
public enum TipoHabitacion {
	simple,
	doble,
	matrimonial,
	triple,
	suite
}
