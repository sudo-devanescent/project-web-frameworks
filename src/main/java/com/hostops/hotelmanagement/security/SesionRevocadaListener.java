package com.hostops.hotelmanagement.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Aplica la revocación de sesiones una vez confirmada la transacción.
 */
@Component
public class SesionRevocadaListener {

	private final SesionService sesionService;

	public SesionRevocadaListener(SesionService sesionService) {
		this.sesionService = sesionService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void alRevocarSesiones(SesionRevocadaEvent evento) {
		sesionService.revocar(evento.idUsuario());
	}
}
