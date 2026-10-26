package com.hostops.hotelmanagement.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.Habitacion;
import com.hostops.hotelmanagement.repositories.BloqueoHabitacionRepository;
import com.hostops.hotelmanagement.repositories.DetalleHuespedRepository;
import com.hostops.hotelmanagement.repositories.DetalleReservaRepository;
import com.hostops.hotelmanagement.repositories.HabitacionRepository;
import com.hostops.hotelmanagement.services.dto.HabitacionComando;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class HabitacionService {

	private static final List<EstadoDetalle> ESTADOS_SIN_OCUPANTES = List.of(
			EstadoDetalle.finalizada, EstadoDetalle.cancelada);

	private final HabitacionRepository habitacionRepository;
	private final DetalleReservaRepository detalleReservaRepository;
	private final DetalleHuespedRepository detalleHuespedRepository;
	private final BloqueoHabitacionRepository bloqueoHabitacionRepository;
	private final TraductorErroresIntegridad traductor;
	private final EntityManager entityManager;

	public HabitacionService(HabitacionRepository habitacionRepository,
			DetalleReservaRepository detalleReservaRepository,
			DetalleHuespedRepository detalleHuespedRepository,
			BloqueoHabitacionRepository bloqueoHabitacionRepository,
			TraductorErroresIntegridad traductor, EntityManager entityManager) {
		this.habitacionRepository = habitacionRepository;
		this.detalleReservaRepository = detalleReservaRepository;
		this.detalleHuespedRepository = detalleHuespedRepository;
		this.bloqueoHabitacionRepository = bloqueoHabitacionRepository;
		this.traductor = traductor;
		this.entityManager = entityManager;
	}

	public List<Habitacion> listar() {
		return habitacionRepository.findAll(Sort.by("numero"));
	}

	public Habitacion obtener(Integer idHabitacion) {
		return habitacionRepository.findById(idHabitacion)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Habitacion crear(HabitacionComando comando) {
		validarComando(comando);
		if (habitacionRepository.existsByNumero(comando.numero().trim())) {
			throw new ConflictoException("Ya existe una habitación con ese número.");
		}

		Habitacion habitacion = new Habitacion();
		if (comando.estado() != null) {
			validarEstadoManual(null, comando.estado());
		}
		aplicar(habitacion, comando, false);
		try {
			Habitacion guardada = habitacionRepository.saveAndFlush(habitacion);
			entityManager.refresh(guardada);
			return guardada;
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Habitacion actualizar(Integer idHabitacion, HabitacionComando comando) {
		Habitacion habitacion = habitacionRepository.findByIdForUpdate(idHabitacion)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));
		validarComando(comando);
		if (habitacionRepository.existsByNumeroAndIdHabitacionNot(comando.numero().trim(), idHabitacion)) {
			throw new ConflictoException("Ya existe una habitación con ese número.");
		}

		if (comando.estado() != null && comando.estado() != habitacion.getEstado()) {
			validarEstadoManual(habitacion.getEstado(), comando.estado());
		}
		if (comando.capacidad() < habitacion.getCapacidad()) {
			validarCapacidadOcupantes(idHabitacion, comando.capacidad());
		}
		aplicar(habitacion, comando, true);
		try {
			Habitacion guardada = habitacionRepository.saveAndFlush(habitacion);
			entityManager.refresh(guardada);
			return guardada;
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Habitacion cambiarEstado(Integer idHabitacion, EstadoHabitacion estado) {
		if (estado == null) {
			throw new DatosInvalidosException("El estado de la habitación es obligatorio.");
		}
		Habitacion habitacion = habitacionRepository.findByIdForUpdate(idHabitacion)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));
		validarEstadoManual(habitacion.getEstado(), estado);

		habitacion.setEstado(estado);
		try {
			Habitacion guardada = habitacionRepository.saveAndFlush(habitacion);
			entityManager.refresh(guardada);
			return guardada;
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idHabitacion) {
		Habitacion habitacion = habitacionRepository.findByIdForUpdate(idHabitacion)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));
		if (detalleReservaRepository.existsByHabitacion_IdHabitacion(idHabitacion)
				|| bloqueoHabitacionRepository.existsByHabitacion_IdHabitacion(idHabitacion)) {
			throw new ConflictoException(
					"La habitación tiene reservas o bloqueos asociados y no se puede eliminar.");
		}

		try {
			habitacionRepository.delete(habitacion);
			habitacionRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private void validarComando(HabitacionComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos de la habitación son obligatorios.");
		}
		if (comando.numero() == null || comando.numero().isBlank()) {
			throw new DatosInvalidosException("El número de habitación es obligatorio.");
		}
		if (comando.numero().trim().length() > 10) {
			throw new DatosInvalidosException("El número de habitación no puede superar 10 caracteres.");
		}
		if (comando.tipo() == null) {
			throw new DatosInvalidosException("El tipo de habitación es obligatorio.");
		}
		if (comando.capacidad() == null || comando.capacidad() <= 0) {
			throw new DatosInvalidosException("La capacidad debe ser mayor que cero.");
		}
		if (comando.tarifa() == null || comando.tarifa().compareTo(BigDecimal.ZERO) < 0) {
			throw new DatosInvalidosException("La tarifa no puede ser negativa.");
		}
	}

	private void validarCapacidadOcupantes(Integer idHabitacion, Short capacidad) {
		boolean superaCapacidad = detalleHuespedRepository
				.contarOcupantesPorDetalleActivo(idHabitacion, ESTADOS_SIN_OCUPANTES)
				.stream()
				.anyMatch(ocupantes -> ocupantes > capacidad);
		if (superaCapacidad) {
			throw new ConflictoException(
					"La nueva capacidad es inferior a los ocupantes de una reserva activa.");
		}
	}

	private void validarEstadoManual(EstadoHabitacion actual, EstadoHabitacion nuevo) {
		if (nuevo == EstadoHabitacion.ocupada) {
			throw new ReglaNegocioException(
					"El estado ocupada solo se asigna mediante un check-in.");
		}
		if (actual == EstadoHabitacion.ocupada) {
			throw new ReglaNegocioException(
					"La habitación ocupada solo se libera mediante un check-out.");
		}
	}

	private void aplicar(Habitacion habitacion, HabitacionComando comando, boolean actualizacion) {
		habitacion.setNumero(comando.numero().trim());
		habitacion.setTipo(comando.tipo());
		habitacion.setCapacidad(comando.capacidad());
		habitacion.setTarifa(comando.tarifa());
		if (!actualizacion || comando.estado() != null) {
			habitacion.setEstado(comando.estado());
		}
	}

}
