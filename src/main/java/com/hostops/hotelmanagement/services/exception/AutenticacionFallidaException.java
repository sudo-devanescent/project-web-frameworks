package com.hostops.hotelmanagement.services.exception;

/**
 * Indica que las credenciales o la sesión no son válidas.
 */
public class AutenticacionFallidaException extends HostOpsException {

	public AutenticacionFallidaException(String mensaje) {
		super(mensaje);
	}

	public AutenticacionFallidaException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

}
