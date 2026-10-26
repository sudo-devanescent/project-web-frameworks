package com.hostops.hotelmanagement.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class ReintentoServicioTests {

	@Test
	void reintentaAnteUnConflictoDeBloqueoYDevuelveElResultado() {
		PlatformTransactionManager gestor = gestorTransaccional();
		ReintentoServicio servicio = new ReintentoServicio(gestor);
		AtomicInteger llamadas = new AtomicInteger();

		String resultado = servicio.ejecutar(() -> {
			if (llamadas.incrementAndGet() < 3) {
				throw new PessimisticLockingFailureException("conflicto");
			}
			return "ok";
		});

		assertEquals("ok", resultado);
		assertEquals(3, llamadas.get());
		verify(gestor, times(1)).commit(any(TransactionStatus.class));
		verify(gestor, times(2)).rollback(any(TransactionStatus.class));
	}

	@Test
	void propagaElConflictoTrasAgotarLosIntentos() {
		PlatformTransactionManager gestor = gestorTransaccional();
		ReintentoServicio servicio = new ReintentoServicio(gestor);
		AtomicInteger llamadas = new AtomicInteger();

		assertThrows(PessimisticLockingFailureException.class, () -> servicio.ejecutar(() -> {
			llamadas.incrementAndGet();
			throw new PessimisticLockingFailureException("conflicto");
		}));

		assertEquals(3, llamadas.get());
		verify(gestor, times(3)).rollback(any(TransactionStatus.class));
	}

	private PlatformTransactionManager gestorTransaccional() {
		PlatformTransactionManager gestor = mock(PlatformTransactionManager.class);
		when(gestor.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
		return gestor;
	}
}
