package com.hostops.hotelmanagement.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.DetalleHuesped;
import com.hostops.hotelmanagement.models.DetalleHuespedId;
import com.hostops.hotelmanagement.models.DetalleReserva;
import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.Habitacion;
import com.hostops.hotelmanagement.models.Huesped;
import com.hostops.hotelmanagement.models.Reserva;
import com.hostops.hotelmanagement.repositories.CancelacionRepository;
import com.hostops.hotelmanagement.repositories.DetalleHuespedRepository;
import com.hostops.hotelmanagement.repositories.DetalleReservaRepository;
import com.hostops.hotelmanagement.repositories.HabitacionRepository;
import com.hostops.hotelmanagement.repositories.HuespedRepository;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.services.dto.DetalleReservaComando;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class DetalleReservaService {

	private final DetalleReservaRepository detalleReservaRepository;
	private final ReservaRepository reservaRepository;
	private final HabitacionRepository habitacionRepository;
	private final HuespedRepository huespedRepository;
	private final DetalleHuespedRepository detalleHuespedRepository;
	private final CancelacionRepository cancelacionRepository;
	private final TraductorErroresIntegridad traductor;
	private final Clock clock;

	public DetalleReservaService(DetalleReservaRepository detalleReservaRepository,
			ReservaRepository reservaRepository, HabitacionRepository habitacionRepository,
			HuespedRepository huespedRepository,
			DetalleHuespedRepository detalleHuespedRepository,
			CancelacionRepository cancelacionRepository,
			TraductorErroresIntegridad traductor, Clock clock) {
		this.detalleReservaRepository = detalleReservaRepository;
		this.reservaRepository = reservaRepository;
		this.habitacionRepository = habitacionRepository;
		this.huespedRepository = huespedRepository;
		this.detalleHuespedRepository = detalleHuespedRepository;
		this.cancelacionRepository = cancelacionRepository;
		this.traductor = traductor;
		this.clock = clock;
	}

	public List<DetalleReserva> listarPorReserva(Integer idReserva) {
		if (!reservaRepository.existsById(idReserva)) {
			throw new RecursoNoEncontradoException("No existe la reserva indicada.");
		}
		return detalleReservaRepository
				.findByReserva_IdReservaOrderByFechaEntradaPrevistaAsc(idReserva);
	}

	public DetalleReserva obtener(Integer idDetalle) {
		return detalleReservaRepository.findById(idDetalle)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el detalle indicado."));
	}

	public List<Huesped> listarHuespedes(Integer idDetalle) {
		if (!detalleReservaRepository.existsById(idDetalle)) {
			throw new RecursoNoEncontradoException("No existe el detalle indicado.");
		}
		return detalleHuespedRepository.findByDetalleReserva_IdDetalle(idDetalle).stream()
				.map(DetalleHuesped::getHuesped)
				.toList();
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public DetalleReserva crear(Integer idReserva, DetalleReservaComando comando) {
		Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
		validarComando(comando);

		Habitacion habitacion = habitacionRepository.findByIdForUpdate(comando.idHabitacion())
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));

		DetalleReserva detalle = new DetalleReserva();
		detalle.setReserva(reserva);
		detalle.setHabitacion(habitacion);
		detalle.setFechaEntradaPrevista(comando.fechaEntradaPrevista());
		detalle.setFechaSalidaPrevista(comando.fechaSalidaPrevista());
		detalle.setTarifaNoche(comando.tarifaNoche());
		detalle.setEstado(EstadoDetalle.confirmada);
		try {
			return detalleReservaRepository.saveAndFlush(detalle);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public DetalleReserva actualizar(Integer idDetalle, DetalleReservaComando comando) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		validarComando(comando);
		if (detalle.getEstado() != EstadoDetalle.confirmada) {
			throw new ReglaNegocioException(
					"Solo se puede modificar un detalle confirmado.");
		}

		Habitacion habitacion = habitacionRepository.findByIdForUpdate(comando.idHabitacion())
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));

		boolean cambiaHabitacion = !habitacion.getIdHabitacion()
				.equals(detalle.getHabitacion().getIdHabitacion());
		detalle.setHabitacion(habitacion);
		detalle.setFechaEntradaPrevista(comando.fechaEntradaPrevista());
		detalle.setFechaSalidaPrevista(comando.fechaSalidaPrevista());
		detalle.setTarifaNoche(cambiaHabitacion
				? habitacion.getTarifa()
				: comando.tarifaNoche());
		try {
			return detalleReservaRepository.saveAndFlush(detalle);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public DetalleReserva registrarCheckin(Integer idDetalle) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		if (detalle.getEstado() != EstadoDetalle.confirmada) {
			throw new ReglaNegocioException(
					"Solo se puede hacer check-in de un detalle confirmado.");
		}
		if (LocalDate.now(clock).isBefore(detalle.getFechaEntradaPrevista())) {
			throw new ReglaNegocioException(
					"Todavía no se ha alcanzado la fecha de entrada de la reserva.");
		}
		if (detalle.getHabitacion().getEstado() != EstadoHabitacion.disponible) {
			throw new ReglaNegocioException(
					"La habitación no está disponible para el check-in.");
		}

		detalle.setCheckinReal(clock.instant());
		detalle.setEstado(EstadoDetalle.en_estadia);
		detalle.getHabitacion().setEstado(EstadoHabitacion.ocupada);
		try {
			return detalleReservaRepository.saveAndFlush(detalle);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public DetalleReserva registrarCheckout(Integer idDetalle) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		if (detalle.getEstado() != EstadoDetalle.en_estadia) {
			throw new ReglaNegocioException(
					"Solo se puede hacer check-out de un detalle en estadía.");
		}

		detalle.setCheckoutReal(clock.instant());
		detalle.setEstado(EstadoDetalle.finalizada);
		detalle.getHabitacion().setEstado(EstadoHabitacion.limpieza);
		try {
			return detalleReservaRepository.saveAndFlush(detalle);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public DetalleReserva actualizarEnEstadia(Integer idDetalle, LocalDate fechaSalida) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		if (detalle.getEstado() != EstadoDetalle.en_estadia) {
			throw new ReglaNegocioException(
					"Solo se puede modificar una estadía en curso.");
		}
		if (fechaSalida == null) {
			throw new DatosInvalidosException("La fecha de salida es obligatoria.");
		}
		if (!fechaSalida.isAfter(detalle.getFechaEntradaPrevista())) {
			throw new DatosInvalidosException(
					"La fecha de salida debe ser posterior a la entrada.");
		}
		if (fechaSalida.isBefore(LocalDate.now(clock))) {
			throw new ReglaNegocioException(
					"La fecha de salida no puede ser anterior a hoy.");
		}

		detalle.setFechaSalidaPrevista(fechaSalida);
		try {
			return detalleReservaRepository.saveAndFlush(detalle);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void reemplazarHuespedes(Integer idDetalle, List<Integer> idsHuespedes,
			Integer idTitularEsperado) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		if (detalle.getEstado() != EstadoDetalle.confirmada
				&& detalle.getEstado() != EstadoDetalle.en_estadia) {
			throw new ReglaNegocioException(
					"No se pueden cambiar ocupantes de un detalle finalizado o cancelado.");
		}
		if (idsHuespedes == null || idsHuespedes.isEmpty()) {
			throw new DatosInvalidosException("Debe indicarse al menos un ocupante.");
		}
		if (idsHuespedes.stream().anyMatch(id -> id == null)) {
			throw new DatosInvalidosException("Los ocupantes no son válidos.");
		}
		Set<Integer> idsUnicos = new HashSet<>(idsHuespedes);
		if (idsUnicos.size() != idsHuespedes.size()) {
			throw new DatosInvalidosException("No se puede repetir un ocupante en el detalle.");
		}
		if (idsHuespedes.size() > detalle.getHabitacion().getCapacidad()) {
			throw new ReglaNegocioException(
					"La cantidad de ocupantes supera la capacidad de la habitación.");
		}

		List<Huesped> huespedes = huespedRepository.findAllById(idsHuespedes);
		if (huespedes.size() != idsUnicos.size()) {
			throw new RecursoNoEncontradoException(
					"Alguno de los huéspedes indicados no existe.");
		}

		Integer idTitular = idTitularEsperado != null
				? idTitularEsperado
				: detalle.getReserva().getHuespedTitular().getIdHuesped();
		Set<Integer> ocupantesResultantes = new HashSet<>(idsUnicos);
		detalleHuespedRepository
				.findByDetalleReserva_Reserva_IdReserva(detalle.getReserva().getIdReserva())
				.stream()
				.filter(linea -> !linea.getDetalleReserva().getIdDetalle().equals(idDetalle))
				.forEach(linea -> ocupantesResultantes.add(linea.getHuesped().getIdHuesped()));
		if (!ocupantesResultantes.contains(idTitular)) {
			throw new ReglaNegocioException(
					"El titular debe figurar en al menos una línea de la reserva.");
		}

		try {
			detalleHuespedRepository.deleteByDetalleReserva_IdDetalle(idDetalle);
			detalleHuespedRepository.flush();
			List<DetalleHuesped> lineas = huespedes.stream().map(huesped -> {
				DetalleHuesped linea = new DetalleHuesped();
				linea.setId(new DetalleHuespedId(idDetalle, huesped.getIdHuesped()));
				linea.setDetalleReserva(detalle);
				linea.setHuesped(huesped);
				return linea;
			}).toList();
			detalleHuespedRepository.saveAllAndFlush(lineas);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public DetalleHuesped agregarHuesped(Integer idDetalle, Integer idHuesped) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		if (detalle.getEstado() != EstadoDetalle.confirmada
				&& detalle.getEstado() != EstadoDetalle.en_estadia) {
			throw new ReglaNegocioException(
					"No se pueden añadir huéspedes a un detalle finalizado o cancelado.");
		}
		Huesped huesped = huespedRepository.findById(idHuesped)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el huésped indicado."));

		if (detalleHuespedRepository
				.findByDetalleReserva_IdDetalleAndHuesped_IdHuesped(idDetalle, idHuesped)
				.isPresent()) {
			throw new ConflictoException("El huésped ya está asociado a este detalle.");
		}

		DetalleHuesped linea = new DetalleHuesped();
		linea.setId(new DetalleHuespedId(idDetalle, idHuesped));
		linea.setDetalleReserva(detalle);
		linea.setHuesped(huesped);
		try {
			return detalleHuespedRepository.saveAndFlush(linea);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminarHuesped(Integer idDetalle, Integer idHuesped) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		if (detalle.getEstado() != EstadoDetalle.confirmada
				&& detalle.getEstado() != EstadoDetalle.en_estadia) {
			throw new ReglaNegocioException(
					"No se pueden quitar huéspedes de un detalle finalizado o cancelado.");
		}

		DetalleHuesped linea = detalleHuespedRepository
				.findByDetalleReserva_IdDetalleAndHuesped_IdHuesped(idDetalle, idHuesped)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"El huésped no está asociado a este detalle."));
		List<DetalleHuesped> lineasRestantes = detalleHuespedRepository
				.findByDetalleReserva_Reserva_IdReserva(detalle.getReserva().getIdReserva());
		lineasRestantes.removeIf(otra -> otra.getId().equals(linea.getId()));
		validarTitularidad(detalle.getReserva(), lineasRestantes);

		try {
			detalleHuespedRepository.delete(linea);
			detalleHuespedRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idDetalle) {
		DetalleReserva detalle = bloquearDetalle(idDetalle);
		if (detalle.getEstado() != EstadoDetalle.confirmada
				|| detalle.getCheckinReal() != null || detalle.getCheckoutReal() != null) {
			throw new ReglaNegocioException(
					"Solo se puede eliminar un detalle confirmado sin check-in ni check-out.");
		}
		if (cancelacionRepository.existsByDetalleReserva_IdDetalle(idDetalle)) {
			throw new ConflictoException("El detalle tiene una cancelación registrada.");
		}

		Reserva reserva = detalle.getReserva();
		List<DetalleHuesped> lineasRestantes = detalleHuespedRepository
				.findByDetalleReserva_Reserva_IdReserva(reserva.getIdReserva());
		lineasRestantes.removeIf(linea -> linea.getId().getIdDetalle().equals(idDetalle));
		validarTitularidad(reserva, lineasRestantes);

		try {
			detalleHuespedRepository.deleteByDetalleReserva_IdDetalle(idDetalle);
			detalleHuespedRepository.flush();
			detalleReservaRepository.delete(detalle);
			detalleReservaRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private DetalleReserva bloquearDetalle(Integer idDetalle) {
		DetalleReserva referencia = detalleReservaRepository.findById(idDetalle)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el detalle indicado."));
		reservaRepository.findByIdForUpdate(referencia.getReserva().getIdReserva())
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
		DetalleReserva detalle = detalleReservaRepository.findByIdForUpdate(idDetalle)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el detalle indicado."));
		habitacionRepository.findByIdForUpdate(detalle.getHabitacion().getIdHabitacion())
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación indicada."));
		return detalle;
	}

	private void validarComando(DetalleReservaComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos del detalle son obligatorios.");
		}
		if (comando.idHabitacion() == null) {
			throw new DatosInvalidosException("La habitación es obligatoria.");
		}
		if (comando.fechaEntradaPrevista() == null || comando.fechaSalidaPrevista() == null) {
			throw new DatosInvalidosException("Las fechas de entrada y salida son obligatorias.");
		}
		if (!comando.fechaSalidaPrevista().isAfter(comando.fechaEntradaPrevista())) {
			throw new DatosInvalidosException("La fecha de salida debe ser posterior a la entrada.");
		}
		if (comando.tarifaNoche() == null
				|| comando.tarifaNoche().compareTo(BigDecimal.ZERO) < 0) {
			throw new DatosInvalidosException("La tarifa por noche no puede ser negativa.");
		}
	}

	private void validarTitularidad(Reserva reserva, List<DetalleHuesped> lineasRestantes) {
		if (lineasRestantes.isEmpty()) {
			return;
		}
		Integer idTitular = reserva.getHuespedTitular().getIdHuesped();
		boolean titularPresente = lineasRestantes.stream()
				.anyMatch(linea -> linea.getHuesped().getIdHuesped().equals(idTitular));
		if (!titularPresente) {
			throw new ReglaNegocioException(
					"El titular debe figurar en al menos una línea de la reserva.");
		}
	}

}
