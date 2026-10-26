package com.hostops.hotelmanagement.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.BloqueoHabitacion;
import com.hostops.hotelmanagement.models.Habitacion;
import com.hostops.hotelmanagement.repositories.BloqueoHabitacionRepository;
import com.hostops.hotelmanagement.repositories.HabitacionRepository;
import com.hostops.hotelmanagement.services.dto.BloqueoHabitacionComando;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class BloqueoHabitacionService {

	private final BloqueoHabitacionRepository bloqueoHabitacionRepository;
	private final HabitacionRepository habitacionRepository;
	private final TraductorErroresIntegridad traductor;

	public BloqueoHabitacionService(BloqueoHabitacionRepository bloqueoHabitacionRepository,
			HabitacionRepository habitacionRepository,
			TraductorErroresIntegridad traductor) {
		this.bloqueoHabitacionRepository = bloqueoHabitacionRepository;
		this.habitacionRepository = habitacionRepository;
		this.traductor = traductor;
	}

	public List<BloqueoHabitacion> listar() {
		return bloqueoHabitacionRepository.findAll(Sort.by("fechaInicio"));
	}

	public List<BloqueoHabitacion> listarPorHabitacion(Integer idHabitacion) {
		if (!habitacionRepository.existsById(idHabitacion)) {
			throw new RecursoNoEncontradoException("No existe la habitación indicada.");
		}
		return bloqueoHabitacionRepository
				.findByHabitacion_IdHabitacionOrderByFechaInicioAsc(idHabitacion);
	}

	public BloqueoHabitacion obtener(Integer idBloqueo) {
		return bloqueoHabitacionRepository.findById(idBloqueo)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el bloqueo indicado."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public BloqueoHabitacion crear(BloqueoHabitacionComando comando) {
		validarComando(comando);
		Habitacion habitacion = bloquearHabitacion(comando.idHabitacion());

		BloqueoHabitacion bloqueo = new BloqueoHabitacion();
		bloqueo.setHabitacion(habitacion);
		bloqueo.setFechaInicio(comando.fechaInicio());
		bloqueo.setFechaFin(comando.fechaFin());
		bloqueo.setMotivo(comando.motivo().trim());
		try {
			return bloqueoHabitacionRepository.saveAndFlush(bloqueo);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public BloqueoHabitacion actualizar(Integer idBloqueo, BloqueoHabitacionComando comando) {
		validarComando(comando);
		BloqueoHabitacion referencia = obtener(idBloqueo);
		Integer idHabitacionAnterior = referencia.getHabitacion().getIdHabitacion();
		bloquearHabitaciones(idHabitacionAnterior, comando.idHabitacion());
		BloqueoHabitacion bloqueo = bloqueoHabitacionRepository.findByIdForUpdate(idBloqueo)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el bloqueo indicado."));
		Habitacion habitacionNueva = obtenerHabitacion(comando.idHabitacion());

		bloqueo.setHabitacion(habitacionNueva);
		bloqueo.setFechaInicio(comando.fechaInicio());
		bloqueo.setFechaFin(comando.fechaFin());
		bloqueo.setMotivo(comando.motivo().trim());
		try {
			return bloqueoHabitacionRepository.saveAndFlush(bloqueo);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idBloqueo) {
		BloqueoHabitacion referencia = obtener(idBloqueo);
		bloquearHabitacion(referencia.getHabitacion().getIdHabitacion());
		BloqueoHabitacion bloqueo = bloqueoHabitacionRepository.findByIdForUpdate(idBloqueo)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el bloqueo indicado."));

		try {
			bloqueoHabitacionRepository.delete(bloqueo);
			bloqueoHabitacionRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private Habitacion bloquearHabitacion(Integer idHabitacion) {
		return habitacionRepository.findByIdForUpdate(idHabitacion)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));
	}

	private void bloquearHabitaciones(Integer idHabitacionActual, Integer idHabitacionNueva) {
		List<Integer> ids = java.util.stream.Stream.of(idHabitacionActual, idHabitacionNueva)
				.distinct()
				.sorted(Comparator.naturalOrder())
				.toList();
		for (Integer id : ids) {
			bloquearHabitacion(id);
		}
	}

	private Habitacion obtenerHabitacion(Integer idHabitacion) {
		return habitacionRepository.findById(idHabitacion)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));
	}

	private void validarComando(BloqueoHabitacionComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos del bloqueo son obligatorios.");
		}
		if (comando.idHabitacion() == null) {
			throw new DatosInvalidosException("La habitación es obligatoria.");
		}
		if (comando.fechaInicio() == null || comando.fechaFin() == null) {
			throw new DatosInvalidosException("Las fechas del bloqueo son obligatorias.");
		}
		if (comando.fechaFin().isBefore(comando.fechaInicio())) {
			throw new DatosInvalidosException(
					"La fecha de fin no puede ser anterior a la fecha de inicio.");
		}
		if (comando.motivo() == null || comando.motivo().isBlank()) {
			throw new DatosInvalidosException("El motivo del bloqueo es obligatorio.");
		}
	}

}
