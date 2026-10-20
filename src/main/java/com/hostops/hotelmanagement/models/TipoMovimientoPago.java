package com.hostops.hotelmanagement.models;

/**
 * Tipos de movimiento de pago. Las constantes coinciden con las etiquetas del
 * ENUM nativo {@code tipo_movimiento_pago} de PostgreSQL (Hibernate escribe el name()).
 */
public enum TipoMovimientoPago {
	anticipo,
	saldo,
	reembolso
}
