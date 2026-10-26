package com.hostops.hotelmanagement.services;

import java.time.Clock;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Canal;
import com.hostops.hotelmanagement.models.Huesped;
import com.hostops.hotelmanagement.models.Reserva;
import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.CanalRepository;
import com.hostops.hotelmanagement.repositories.DetalleHuespedRepository;
import com.hostops.hotelmanagement.repositories.DetalleReservaRepository;
import com.hostops.hotelmanagement.repositories.HuespedRepository;
import com.hostops.hotelmanagement.repositories.PagoRepository;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.services.dto.ReservaActualizarComando;
import com.hostops.hotelmanagement.services.dto.ReservaCrearComando;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class ReservaService {

	private final ReservaRepository reservaRepository;
	private final HuespedRepository huespedRepository;
	private final CanalRepository canalRepository;
	private final UsuarioRepository usuarioRepository;
	private final DetalleReservaRepository detalleReservaRepository;
	private final DetalleHuespedRepository detalleHuespedRepository;
	private final PagoRepository pagoRepository;
	private final TraductorErroresIntegridad traductor;
	private final EntityManager entityManager;
	private final Clock clock;

	public ReservaService(ReservaRepository reservaRepository, HuespedRepository huespedRepository,
			CanalRepository canalRepository, UsuarioRepository usuarioRepository,
			DetalleReservaRepository detalleReservaRepository,
			DetalleHuespedRepository detalleHuespedRepository, PagoRepository pagoRepository,
			TraductorErroresIntegridad traductor, EntityManager entityManager, Clock clock) {
		this.reservaRepository = reservaRepository;
		this.huespedRepository = huespedRepository;
		this.canalRepository = canalRepository;
		this.usuarioRepository = usuarioRepository;
		this.detalleReservaRepository = detalleReservaRepository;
		this.detalleHuespedRepository = detalleHuespedRepository;
		this.pagoRepository = pagoRepository;
		this.traductor = traductor;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	public List<Reserva> listar() {
		return reservaRepository.findAllByOrderByFechaCreacionDesc();
	}

	public List<Reserva> listarPorTitular(Integer idHuesped) {
		return reservaRepository.findByHuespedTitular_IdHuespedOrderByFechaCreacionDesc(idHuesped);
	}

	public Reserva obtener(Integer idReserva) {
		return reservaRepository.findById(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Reserva crear(ReservaCrearComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos de la reserva son obligatorios.");
		}
		if (comando.idHuespedTitular() == null) {
			throw new DatosInvalidosException("El huésped titular es obligatorio.");
		}
		if (comando.idCanal() == null) {
			throw new DatosInvalidosException("El canal de la reserva es obligatorio.");
		}
		if (comando.idUsuarioCreador() == null) {
			throw new DatosInvalidosException("El usuario creador es obligatorio.");
		}

		Reserva reserva = new Reserva();
		reserva.setHuespedTitular(obtenerHuesped(comando.idHuespedTitular()));
		reserva.setCanal(obtenerCanal(comando.idCanal()));
		reserva.setUsuarioCreador(obtenerUsuario(comando.idUsuarioCreador()));
		reserva.setFechaCreacion(clock.instant());
		try {
			return reservaRepository.saveAndFlush(reserva);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Reserva actualizar(Integer idReserva, ReservaActualizarComando comando) {
		Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
		if (comando == null) {
			throw new DatosInvalidosException("Los datos de la reserva son obligatorios.");
		}
		if (comando.idHuespedTitular() == null) {
			throw new DatosInvalidosException("El huésped titular es obligatorio.");
		}
		if (comando.idCanal() == null) {
			throw new DatosInvalidosException("El canal de la reserva es obligatorio.");
		}
		if (comando.idUsuarioModificador() == null) {
			throw new DatosInvalidosException("El usuario modificador es obligatorio.");
		}

		Huesped titular = obtenerHuesped(comando.idHuespedTitular());
		if (detalleHuespedRepository.existsByDetalleReserva_Reserva_IdReserva(idReserva)
				&& !detalleHuespedRepository
						.existsByDetalleReserva_Reserva_IdReservaAndHuesped_IdHuesped(
								idReserva, titular.getIdHuesped())) {
			throw new ReglaNegocioException(
					"El titular debe figurar en al menos una línea de la reserva.");
		}

		reserva.setHuespedTitular(titular);
		reserva.setCanal(obtenerCanal(comando.idCanal()));
		reserva.setUsuarioModificador(obtenerUsuario(comando.idUsuarioModificador()));
		try {
			Reserva actualizada = reservaRepository.saveAndFlush(reserva);
			entityManager.refresh(actualizada);
			return actualizada;
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idReserva) {
		Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
		if (detalleReservaRepository.existsByReserva_IdReserva(idReserva)) {
			throw new ConflictoException(
					"La reserva tiene detalles asociados y no se puede eliminar.");
		}
		if (pagoRepository.existsByReserva_IdReserva(idReserva)) {
			throw new ConflictoException(
					"La reserva tiene pagos asociados y no se puede eliminar.");
		}

		try {
			reservaRepository.delete(reserva);
			reservaRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private Huesped obtenerHuesped(Integer idHuesped) {
		return huespedRepository.findById(idHuesped)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el huésped indicado."));
	}

	private Canal obtenerCanal(Integer idCanal) {
		return canalRepository.findById(idCanal)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el canal indicado."));
	}

	private Usuario obtenerUsuario(Integer idUsuario) {
		return usuarioRepository.findById(idUsuario)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario indicado."));
	}

}
