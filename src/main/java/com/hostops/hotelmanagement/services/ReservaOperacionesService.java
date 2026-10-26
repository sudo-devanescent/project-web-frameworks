package com.hostops.hotelmanagement.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Canal;
import com.hostops.hotelmanagement.models.DetalleReserva;
import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.models.Habitacion;
import com.hostops.hotelmanagement.models.Huesped;
import com.hostops.hotelmanagement.models.Reserva;
import com.hostops.hotelmanagement.models.TipoMovimientoPago;
import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.CanalRepository;
import com.hostops.hotelmanagement.repositories.DetalleReservaRepository;
import com.hostops.hotelmanagement.repositories.HabitacionRepository;
import com.hostops.hotelmanagement.repositories.HuespedRepository;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.services.dto.DetalleReservaComando;
import com.hostops.hotelmanagement.services.dto.PagoComando;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaCrearComando;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaDto;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaEditarComando;
import com.hostops.hotelmanagement.services.dto.ReservaCrearComando;
import com.hostops.hotelmanagement.services.dto.ReservaLineaComando;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;

import jakarta.persistence.EntityManager;

/**
 * Coordina la creación y edición atómica de reservas completas.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class ReservaOperacionesService {

	private final ReservaService reservaService;
	private final DetalleReservaService detalleReservaService;
	private final PagoService pagoService;
	private final ConsultasApiService consultasApiService;
	private final ReservaRepository reservaRepository;
	private final DetalleReservaRepository detalleReservaRepository;
	private final HabitacionRepository habitacionRepository;
	private final HuespedRepository huespedRepository;
	private final CanalRepository canalRepository;
	private final UsuarioRepository usuarioRepository;
	private final EntityManager entityManager;

	public ReservaOperacionesService(ReservaService reservaService,
			DetalleReservaService detalleReservaService, PagoService pagoService,
			ConsultasApiService consultasApiService, ReservaRepository reservaRepository,
			DetalleReservaRepository detalleReservaRepository,
			HabitacionRepository habitacionRepository, HuespedRepository huespedRepository,
			CanalRepository canalRepository, UsuarioRepository usuarioRepository,
			EntityManager entityManager) {
		this.reservaService = reservaService;
		this.detalleReservaService = detalleReservaService;
		this.pagoService = pagoService;
		this.consultasApiService = consultasApiService;
		this.reservaRepository = reservaRepository;
		this.detalleReservaRepository = detalleReservaRepository;
		this.habitacionRepository = habitacionRepository;
		this.huespedRepository = huespedRepository;
		this.canalRepository = canalRepository;
		this.usuarioRepository = usuarioRepository;
		this.entityManager = entityManager;
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ReservaCompletaDto crear(ReservaCompletaCrearComando comando) {
		validarCreacion(comando);
		Reserva reserva = reservaService.crear(new ReservaCrearComando(
				comando.idHuespedTitular(),
				comando.idCanal(),
				comando.idUsuarioCreador()));

		comando.detalles().forEach(this::validarLinea);
		Map<Integer, Habitacion> habitaciones = bloquearHabitaciones(comando.detalles());
		List<ReservaLineaComando> lineas = new ArrayList<>(comando.detalles());
		lineas.sort(Comparator.comparing(
				linea -> !linea.idHuespedes().contains(comando.idHuespedTitular())));
		for (ReservaLineaComando linea : lineas) {
			Habitacion habitacion = habitaciones.get(linea.idHabitacion());
			DetalleReserva detalle = detalleReservaService.crear(
					reserva.getIdReserva(),
					new DetalleReservaComando(
							habitacion.getIdHabitacion(),
							linea.fechaEntradaPrevista(),
							linea.fechaSalidaPrevista(),
							habitacion.getTarifa()));
			detalleReservaService.reemplazarHuespedes(
					detalle.getIdDetalle(),
					linea.idHuespedes(),
					comando.idHuespedTitular());
		}

		BigDecimal montoAnticipo = comando.montoAnticipo() == null
				? BigDecimal.ZERO
				: comando.montoAnticipo();
		if (montoAnticipo.compareTo(BigDecimal.ZERO) > 0) {
			if (comando.metodoAnticipo() == null) {
				throw new DatosInvalidosException(
						"El método del anticipo es obligatorio cuando se registra un monto.");
			}
			pagoService.registrar(reserva.getIdReserva(), new PagoComando(
					TipoMovimientoPago.anticipo,
					comando.metodoAnticipo(),
					montoAnticipo,
					reserva.getUsuarioCreador().getIdUsuario(),
					"Anticipo registrado al crear la reserva."));
		}

		return consultasApiService.obtenerReservaCompleta(reserva.getIdReserva());
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ReservaCompletaDto editar(Integer idReserva, Integer idDetalle,
			ReservaCompletaEditarComando comando) {
		validarEdicion(comando);
		Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe la reserva indicada."));
		DetalleReserva detalle = detalleReservaRepository.findByIdForUpdate(idDetalle)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el detalle indicado."));
		if (!detalle.getReserva().getIdReserva().equals(idReserva)) {
			throw new RecursoNoEncontradoException(
					"El detalle no pertenece a la reserva indicada.");
		}
		if (detalle.getEstado() != EstadoDetalle.confirmada
				&& detalle.getEstado() != EstadoDetalle.en_estadia) {
			throw new ReglaNegocioException(
					"Solo se pueden modificar reservas confirmadas o en estadía.");
		}

		Huesped titular = huespedRepository.findById(comando.idHuespedTitular())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el huésped titular indicado."));
		Canal canal = canalRepository.findById(comando.idCanal())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el canal indicado."));
		Usuario modificador = usuarioRepository.findById(comando.idUsuarioModificador())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el usuario indicado."));

		if (detalle.getEstado() == EstadoDetalle.confirmada) {
			actualizarDetalleConfirmado(detalle, comando);
		} else {
			actualizarDetalleEnEstadia(detalle, comando);
		}
		detalleReservaService.reemplazarHuespedes(
				idDetalle, comando.idHuespedes(), titular.getIdHuesped());

		reserva.setHuespedTitular(titular);
		reserva.setCanal(canal);
		reserva.setUsuarioModificador(modificador);
		Reserva actualizada = reservaRepository.saveAndFlush(reserva);
		entityManager.refresh(actualizada);
		return consultasApiService.obtenerReservaCompleta(idReserva);
	}

	private void actualizarDetalleConfirmado(DetalleReserva detalle,
			ReservaCompletaEditarComando comando) {
		detalleReservaService.actualizar(detalle.getIdDetalle(),
				new DetalleReservaComando(
						comando.idHabitacion(),
						comando.fechaEntradaPrevista(),
						comando.fechaSalidaPrevista(),
						detalle.getTarifaNoche()));
	}

	private void actualizarDetalleEnEstadia(DetalleReserva detalle,
			ReservaCompletaEditarComando comando) {
		if (!detalle.getHabitacion().getIdHabitacion().equals(comando.idHabitacion())) {
			throw new ReglaNegocioException(
					"No se puede cambiar la habitación de una estadía en curso.");
		}
		if (!detalle.getFechaEntradaPrevista().equals(comando.fechaEntradaPrevista())) {
			throw new ReglaNegocioException(
					"No se puede cambiar la fecha de entrada de una estadía en curso.");
		}
		detalleReservaService.actualizarEnEstadia(
				detalle.getIdDetalle(), comando.fechaSalidaPrevista());
	}

	private Map<Integer, Habitacion> bloquearHabitaciones(List<ReservaLineaComando> detalles) {
		List<Integer> ids = detalles.stream().map(ReservaLineaComando::idHabitacion)
				.distinct().sorted().toList();
		return ids.stream()
				.map(id -> habitacionRepository.findByIdForUpdate(id)
						.orElseThrow(() -> new RecursoNoEncontradoException(
								"No existe la habitación indicada.")))
				.collect(Collectors.toMap(Habitacion::getIdHabitacion, Function.identity()));
	}

	private void validarCreacion(ReservaCompletaCrearComando comando) {
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
		if (comando.detalles() == null || comando.detalles().isEmpty()) {
			throw new DatosInvalidosException("Debe indicarse al menos una habitación.");
		}
		if (comando.montoAnticipo() != null
				&& comando.montoAnticipo().compareTo(BigDecimal.ZERO) < 0) {
			throw new DatosInvalidosException("El anticipo no puede ser negativo.");
		}
	}

	private void validarLinea(ReservaLineaComando linea) {
		if (linea == null) {
			throw new DatosInvalidosException("Las líneas de la reserva son obligatorias.");
		}
		if (linea.idHabitacion() == null || linea.fechaEntradaPrevista() == null
				|| linea.fechaSalidaPrevista() == null) {
			throw new DatosInvalidosException(
					"La habitación y las fechas de cada línea son obligatorias.");
		}
		if (linea.idHuespedes() == null || linea.idHuespedes().isEmpty()) {
			throw new DatosInvalidosException("Cada habitación debe tener ocupantes.");
		}
	}

	private void validarEdicion(ReservaCompletaEditarComando comando) {
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
		if (comando.idHabitacion() == null) {
			throw new DatosInvalidosException("La habitación es obligatoria.");
		}
		if (comando.fechaEntradaPrevista() == null || comando.fechaSalidaPrevista() == null) {
			throw new DatosInvalidosException("Las fechas de entrada y salida son obligatorias.");
		}
		if (comando.fechaSalidaPrevista().isBefore(comando.fechaEntradaPrevista())
				|| comando.fechaSalidaPrevista().equals(comando.fechaEntradaPrevista())) {
			throw new DatosInvalidosException(
					"La fecha de salida debe ser posterior a la entrada.");
		}
	}

}
