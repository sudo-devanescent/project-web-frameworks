package com.hostops.hotelmanagement.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Pago;
import com.hostops.hotelmanagement.models.Reserva;
import com.hostops.hotelmanagement.models.TipoMovimientoPago;
import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.PagoRepository;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.repositories.VistasConsultaRepository;
import com.hostops.hotelmanagement.services.dto.CompensacionPagoComando;
import com.hostops.hotelmanagement.services.dto.PagoComando;
import com.hostops.hotelmanagement.services.dto.SaldoReservaDto;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.SaldoPendienteException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class PagoService {

	private final PagoRepository pagoRepository;
	private final ReservaRepository reservaRepository;
	private final UsuarioRepository usuarioRepository;
	private final VistasConsultaRepository vistasConsultaRepository;
	private final TraductorErroresIntegridad traductor;
	private final Clock clock;

	public PagoService(PagoRepository pagoRepository, ReservaRepository reservaRepository,
			UsuarioRepository usuarioRepository,
			VistasConsultaRepository vistasConsultaRepository,
			TraductorErroresIntegridad traductor, Clock clock) {
		this.pagoRepository = pagoRepository;
		this.reservaRepository = reservaRepository;
		this.usuarioRepository = usuarioRepository;
		this.vistasConsultaRepository = vistasConsultaRepository;
		this.traductor = traductor;
		this.clock = clock;
	}

	public List<Pago> listarPorReserva(Integer idReserva) {
		if (!reservaRepository.existsById(idReserva)) {
			throw new RecursoNoEncontradoException("No existe la reserva indicada.");
		}
		return pagoRepository.findByReserva_IdReservaOrderByFechaPagoAsc(idReserva);
	}

	public Pago obtener(Integer idPago) {
		return pagoRepository.findById(idPago)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el pago indicado."));
	}

	public SaldoReservaDto obtenerSaldo(Integer idReserva) {
		if (!reservaRepository.existsById(idReserva)) {
			throw new RecursoNoEncontradoException("No existe la reserva indicada.");
		}
		return vistasConsultaRepository.obtenerSaldoReserva(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No se pudo calcular el saldo de la reserva."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Pago registrar(Integer idReserva, PagoComando comando) {
		Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
		validarComando(comando);

		SaldoReservaDto saldo = vistasConsultaRepository.obtenerSaldoReserva(idReserva)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No se pudo calcular el saldo de la reserva."));
		if (comando.tipo() == TipoMovimientoPago.reembolso) {
			if (comando.monto().compareTo(saldo.reembolsoPendiente()) > 0) {
				throw new SaldoPendienteException(
						"El reembolso supera el importe pendiente de devolución.",
						saldo.reembolsoPendiente());
			}
		} else if (comando.monto().compareTo(saldo.saldoPendiente()) > 0) {
			throw new SaldoPendienteException(
					"El pago supera el saldo pendiente de la reserva.",
					saldo.saldoPendiente());
		}

		Pago pago = new Pago();
		pago.setReserva(reserva);
		pago.setUsuario(resolverUsuario(comando.idUsuario(), reserva));
		pago.setTipo(comando.tipo());
		pago.setMetodo(comando.metodo());
		pago.setFechaPago(clock.instant());
		pago.setMonto(comando.monto());
		pago.setMotivo(comando.motivo() == null || comando.motivo().isBlank()
				? null
				: comando.motivo().trim());
		try {
			return pagoRepository.saveAndFlush(pago);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Pago compensar(Integer idPago, CompensacionPagoComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos de la compensación son obligatorios.");
		}
		Pago referencia = obtener(idPago);
		Reserva reserva = reservaRepository
				.findByIdForUpdate(referencia.getReserva().getIdReserva())
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva indicada."));
		Pago original = pagoRepository.findByIdForUpdate(idPago)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el pago indicado."));

		if (original.getPagoOriginal() != null) {
			throw new ReglaNegocioException(
					"Solo se pueden compensar movimientos originales, no compensaciones.");
		}
		if (pagoRepository.existsByPagoOriginal_IdPago(idPago)) {
			throw new ReglaNegocioException(
					"El movimiento ya tiene una compensación registrada.");
		}

		TipoMovimientoPago tipoCompensacion = original.getTipo() == TipoMovimientoPago.reembolso
				? TipoMovimientoPago.saldo
				: TipoMovimientoPago.reembolso;
		BigDecimal monto = comando.monto() == null ? original.getMonto() : comando.monto();
		if (monto.compareTo(BigDecimal.ZERO) <= 0) {
			throw new DatosInvalidosException("El monto de la compensación debe ser mayor que cero.");
		}
		if (monto.compareTo(original.getMonto()) > 0) {
			throw new ReglaNegocioException(
					"La compensación no puede superar el monto del movimiento original.");
		}

		Pago compensacion = new Pago();
		compensacion.setReserva(reserva);
		compensacion.setUsuario(resolverUsuario(comando.idUsuario(), reserva));
		compensacion.setPagoOriginal(original);
		compensacion.setTipo(tipoCompensacion);
		compensacion.setMetodo(comando.metodo() == null ? original.getMetodo() : comando.metodo());
		compensacion.setFechaPago(clock.instant());
		compensacion.setMonto(monto);
		try {
			return pagoRepository.saveAndFlush(compensacion);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private Usuario resolverUsuario(Integer idUsuario, Reserva reserva) {
		if (idUsuario == null) {
			return reserva.getUsuarioCreador();
		}
		return usuarioRepository.findById(idUsuario)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el usuario indicado."));
	}

	private void validarComando(PagoComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos del pago son obligatorios.");
		}
		if (comando.tipo() == null) {
			throw new DatosInvalidosException("El tipo de movimiento es obligatorio.");
		}
		if (comando.metodo() == null) {
			throw new DatosInvalidosException("El método de pago es obligatorio.");
		}
		if (comando.monto() == null || comando.monto().compareTo(BigDecimal.ZERO) <= 0) {
			throw new DatosInvalidosException("El monto debe ser mayor que cero.");
		}
	}

}
