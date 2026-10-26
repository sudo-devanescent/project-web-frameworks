package com.hostops.hotelmanagement.services.exception;

/**
 * Indica que los datos recibidos no son válidos para la operación solicitada.
 */
public class DatosInvalidosException extends HostOpsException {

	public DatosInvalidosException(String mensaje) {
		super(mensaje);
	}

}
