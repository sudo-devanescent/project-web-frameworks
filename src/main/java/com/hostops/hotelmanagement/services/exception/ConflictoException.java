package com.hostops.hotelmanagement.services.exception;

/**
 * Indica que la operación entra en conflicto con el estado actual de los datos.
 */
public class ConflictoException extends HostOpsException {

	public ConflictoException(String mensaje) {
		super(mensaje);
	}

	public ConflictoException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

}
