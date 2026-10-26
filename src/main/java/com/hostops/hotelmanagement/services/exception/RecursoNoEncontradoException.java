package com.hostops.hotelmanagement.services.exception;

/**
 * Indica que el recurso solicitado no existe.
 */
public class RecursoNoEncontradoException extends HostOpsException {

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}

}
