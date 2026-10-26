package com.hostops.hotelmanagement.controllers.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hostops.hotelmanagement.controllers.api.dto.CancelacionRequest;
import com.hostops.hotelmanagement.controllers.api.dto.PagoAnticipoRequest;
import com.hostops.hotelmanagement.controllers.api.dto.ReservaCrearRequest;
import com.hostops.hotelmanagement.controllers.api.dto.ReservaDetalleRequest;
import com.hostops.hotelmanagement.controllers.api.dto.ReservaEditarRequest;
import com.hostops.hotelmanagement.models.DetalleReserva;
import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.security.SeguridadUtils;
import com.hostops.hotelmanagement.services.CancelacionService;
import com.hostops.hotelmanagement.services.ConsultasApiService;
import com.hostops.hotelmanagement.services.DetalleReservaService;
import com.hostops.hotelmanagement.services.ReintentoServicio;
import com.hostops.hotelmanagement.services.ReservaOperacionesService;
import com.hostops.hotelmanagement.services.dto.CancelacionComando;
import com.hostops.hotelmanagement.services.dto.CancelacionDto;
import com.hostops.hotelmanagement.services.dto.DetalleReservaDto;
import com.hostops.hotelmanagement.services.dto.DetalleReservaListadoDto;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaCrearComando;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaDto;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaEditarComando;
import com.hostops.hotelmanagement.services.dto.ReservaDto;
import com.hostops.hotelmanagement.services.dto.ReservaLineaComando;

import jakarta.validation.Valid;

/**
 * Expone reservas, detalles, check-in, check-out y cancelaciones.
 */
@RestController
@RequestMapping("/api")
@Validated
public class ReservaApiController {

	private final ReservaOperacionesService reservaOperacionesService;
	private final DetalleReservaService detalleReservaService;
	private final CancelacionService cancelacionService;
	private final ConsultasApiService consultasApiService;
	private final ReintentoServicio reintentoServicio;

	public ReservaApiController(ReservaOperacionesService reservaOperacionesService,
			DetalleReservaService detalleReservaService,
			CancelacionService cancelacionService,
			ConsultasApiService consultasApiService,
			ReintentoServicio reintentoServicio) {
		this.reservaOperacionesService = reservaOperacionesService;
		this.detalleReservaService = detalleReservaService;
		this.cancelacionService = cancelacionService;
		this.consultasApiService = consultasApiService;
		this.reintentoServicio = reintentoServicio;
	}

	@GetMapping("/reservas/{id}")
	public ReservaDto obtenerReserva(@PathVariable Integer id) {
		return consultasApiService.obtenerReserva(id);
	}

	@PostMapping("/reservas")
	public ResponseEntity<ReservaCompletaDto> crear(
			@Valid @RequestBody ReservaCrearRequest solicitud) {
		PagoAnticipoRequest anticipo = solicitud.pagoAnticipo();
		List<ReservaLineaComando> lineas = solicitud.detalles().stream()
				.map(linea -> new ReservaLineaComando(
						linea.idHabitacion(),
						linea.fechaEntradaPrevista(),
						linea.fechaSalidaPrevista(),
						linea.huespedes()))
				.toList();
		ReservaCompletaDto creada = reintentoServicio.ejecutar(
				() -> reservaOperacionesService.crear(
						new ReservaCompletaCrearComando(
								solicitud.idHuespedTitular(),
								solicitud.idCanal(),
								SeguridadUtils.idUsuarioActual(),
								lineas,
								anticipo == null ? null : anticipo.metodo(),
								anticipo == null ? null : anticipo.monto())));
		return ResponseEntity.created(
				URI.create("/api/reservas/" + creada.reserva().idReserva()))
				.body(creada);
	}

	@PutMapping("/reservas/{reservaId}/detalles/{detalleId}")
	public ReservaCompletaDto editar(@PathVariable Integer reservaId,
			@PathVariable Integer detalleId,
			@Valid @RequestBody ReservaEditarRequest solicitud) {
		ReservaDetalleRequest detalle = solicitud.detalle();
		return reintentoServicio.ejecutar(
				() -> reservaOperacionesService.editar(
						reservaId,
						detalleId,
						new ReservaCompletaEditarComando(
								solicitud.idHuespedTitular(),
								solicitud.idCanal(),
								SeguridadUtils.idUsuarioActual(),
								detalle.idHabitacion(),
								detalle.fechaEntradaPrevista(),
								detalle.fechaSalidaPrevista(),
								detalle.huespedes())));
	}

	@GetMapping("/detalles-reserva")
	public List<DetalleReservaListadoDto> listarDetalles(
			@RequestParam(required = false) Integer reservaId,
			@RequestParam(required = false) Integer habitacionId,
			@RequestParam(required = false) EstadoDetalle estado) {
		return consultasApiService.listarDetalles(reservaId, habitacionId, estado);
	}

	@GetMapping("/detalles-reserva/{id}")
	public DetalleReservaDto obtenerDetalle(@PathVariable Integer id) {
		return consultasApiService.obtenerDetalle(id);
	}

	@PostMapping("/detalles-reserva/{id}/checkin")
	public DetalleReservaDto checkin(@PathVariable Integer id) {
		detalleReservaService.registrarCheckin(id);
		return consultasApiService.obtenerDetalle(id);
	}

	@PostMapping("/detalles-reserva/{id}/checkout")
	public DetalleReservaDto checkout(@PathVariable Integer id) {
		detalleReservaService.registrarCheckout(id);
		return consultasApiService.obtenerDetalle(id);
	}

	@PostMapping("/detalles-reserva/{id}/cancelaciones")
	public ResponseEntity<CancelacionDto> cancelar(@PathVariable Integer id,
			@Valid @RequestBody CancelacionRequest solicitud) {
		DetalleReserva detalle = detalleReservaService.obtener(id);
		cancelacionService.cancelar(id, new CancelacionComando(
				solicitud.motivo(),
				solicitud.penalidad(),
				SeguridadUtils.idUsuarioActual()));
		CancelacionDto cancelacion = consultasApiService.obtenerCancelacion(id);
		return ResponseEntity.created(URI.create(
				"/api/detalles-reserva/" + detalle.getIdDetalle() + "/cancelacion"))
				.body(cancelacion);
	}

	@GetMapping("/detalles-reserva/{id}/cancelacion")
	public CancelacionDto obtenerCancelacion(@PathVariable Integer id) {
		return consultasApiService.obtenerCancelacion(id);
	}

}
