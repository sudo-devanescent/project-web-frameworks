package com.hostops.hotelmanagement.models;

/**
 * Estados de cada detalle de reserva. Las constantes coinciden con las etiquetas
 * del ENUM nativo {@code estado_detalle} de PostgreSQL (Hibernate escribe el name()).
 */
public enum EstadoDetalle {
	confirmada,
	en_estadia,
	finalizada,
	cancelada
}
