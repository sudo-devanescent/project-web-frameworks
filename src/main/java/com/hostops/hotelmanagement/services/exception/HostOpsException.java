package com.hostops.hotelmanagement.services.exception;

/**
 * Excepción base de las reglas de negocio y errores controlados de HostOps.
 */
public class HostOpsException extends RuntimeException {

	public HostOpsException(String mensaje) {
		super(mensaje);
	}

	public HostOpsException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

}
