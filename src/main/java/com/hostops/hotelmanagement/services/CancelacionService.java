package com.hostops.hotelmanagement.services;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Cancelacion;
import com.hostops.hotelmanagement.models.DetalleReserva;
import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.models.Reserva;
import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.CancelacionRepository;
import com.hostops.hotelmanagement.repositories.DetalleReservaRepository;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.services.dto.CancelacionComando;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class CancelacionService {

	private final CancelacionRepository cancelacionRepository;
	private final DetalleReservaRepository detalleReservaRepository;
	private final ReservaRepository reservaRepository;
	private final UsuarioRepository usuarioRepository;
	private final TraductorErroresIntegridad traductor;
	private final EntityManager entityManager;
	private final Clock clock;

	public CancelacionService(CancelacionRepository cancelacionRepository,
			DetalleReservaRepository detalleReservaRepository,
			ReservaRepository reservaRepository, UsuarioRepository usuarioRepository,
			TraductorErroresIntegridad traductor, EntityManager entityManager, Clock clock) {
		this.cancelacionRepository = cancelacionRepository;
		this.detalleReservaRepository = detalleReservaRepository;
		this.reservaRepository = reservaRepository;
		this.usuarioRepository = usuarioRepository;
		this.traductor = traductor;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	public Cancelacion obtenerPorDetalle(Integer idDetalle) {
		return cancelacionRepository.findByDetalleReserva_IdDetalle(idDetalle)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe una cancelación para el detalle indicado."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Cancelacion cancelar(Integer idDetalle, CancelacionComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos de la cancelación son obligatorios.");
		}
		validarMotivo(comando.motivo());
		BigDecimal penalidad = comando.penalidad() == null ? BigDecimal.ZERO : comando.penalidad();
		if (penalidad.compareTo(BigDecimal.ZERO) < 0) {
			throw new DatosInvalidosException("La penalidad no puede ser negativa.");
		}

		DetalleReserva referencia = detalleReservaRepository.findById(idDetalle)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el detalle indicado."));
		Reserva reserva = reservaRepository
				.findByIdForUpdate(referencia.getReserva().getIdReserva())
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
		DetalleReserva detalle = detalleReservaRepository.findByIdForUpdate(idDetalle)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el detalle indicado."));

		if (cancelacionRepository.existsByDetalleReserva_IdDetalle(idDetalle)) {
			throw new ConflictoException("El detalle ya tiene una cancelación registrada.");
		}
		if (detalle.getEstado() != EstadoDetalle.confirmada) {
			throw new ReglaNegocioException("Solo se puede cancelar un detalle confirmado.");
		}

		Usuario usuario = comando.idUsuarioModificador() == null
				? reserva.getUsuarioCreador()
				: usuarioRepository.findById(comando.idUsuarioModificador())
						.orElseThrow(() -> new RecursoNoEncontradoException(
								"No existe el usuario indicado."));
		if (comando.idUsuarioModificador() != null) {
			reserva.setUsuarioModificador(usuario);
			reservaRepository.save(reserva);
		}

		Cancelacion cancelacion = new Cancelacion();
		cancelacion.setDetalleReserva(detalle);
		cancelacion.setUsuario(usuario);
		cancelacion.setFechaCancelacion(clock.instant());
		cancelacion.setMotivo(comando.motivo().trim());
		cancelacion.setPenalidad(penalidad);
		try {
			Cancelacion guardada = cancelacionRepository.saveAndFlush(cancelacion);
			entityManager.refresh(detalle);
			return guardada;
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private void validarMotivo(String motivo) {
		if (motivo == null || motivo.isBlank()) {
			throw new DatosInvalidosException("El motivo de la cancelación es obligatorio.");
		}
	}

}
