package com.hostops.hotelmanagement.services.exception;

/**
 * Indica que la operación incumple una regla de negocio.
 */
public class ReglaNegocioException extends HostOpsException {

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}

	public ReglaNegocioException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

}
