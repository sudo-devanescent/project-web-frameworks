package com.hostops.hotelmanagement.models;

/**
 * Métodos de pago. Las constantes coinciden con las etiquetas del ENUM nativo
 * {@code metodo_pago} de PostgreSQL (Hibernate escribe el name()).
 */
public enum MetodoPago {
	efectivo,
	tarjeta,
	transferencia,
	billetera_digital
}
