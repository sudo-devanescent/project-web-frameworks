package com.hostops.hotelmanagement.services;

import java.util.function.Supplier;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ejecuta una operación transaccional con reintentos ante conflictos de bloqueo.
 */
@Service
public class ReintentoServicio {

	private static final int INTENTOS = 3;
	private static final long ESPERA_BASE_MS = 50L;

	private final TransactionTemplate transactionTemplate;

	public ReintentoServicio(PlatformTransactionManager transactionManager) {
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplate.setPropagationBehavior(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public <T> T ejecutar(Supplier<T> operacion) {
		PessimisticLockingFailureException ultimoError = null;
		for (int intento = 0; intento < INTENTOS; intento++) {
			try {
				return transactionTemplate.execute(estado -> operacion.get());
			} catch (PessimisticLockingFailureException excepcion) {
				ultimoError = excepcion;
				if (intento < INTENTOS - 1) {
					esperar(ESPERA_BASE_MS * (intento + 1));
				}
			}
		}
		throw ultimoError;
	}

	private void esperar(long milisegundos) {
		try {
			Thread.sleep(milisegundos);
		} catch (InterruptedException excepcion) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Reintento interrumpido.", excepcion);
		}
	}
}
