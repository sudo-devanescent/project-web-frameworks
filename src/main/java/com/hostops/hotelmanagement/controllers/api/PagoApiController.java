package com.hostops.hotelmanagement.controllers.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hostops.hotelmanagement.controllers.api.dto.CompensacionPagoRequest;
import com.hostops.hotelmanagement.controllers.api.dto.PagoRequest;
import com.hostops.hotelmanagement.models.Pago;
import com.hostops.hotelmanagement.security.SeguridadUtils;
import com.hostops.hotelmanagement.services.ConsultasApiService;
import com.hostops.hotelmanagement.services.PagoService;
import com.hostops.hotelmanagement.services.dto.CompensacionPagoComando;
import com.hostops.hotelmanagement.services.dto.PagoComando;
import com.hostops.hotelmanagement.services.dto.PagoDto;
import com.hostops.hotelmanagement.services.dto.SaldoReservaDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Expone saldos, movimientos de pago y compensaciones.
 */
@RestController
@RequestMapping("/api")
@Validated
public class PagoApiController {

	private final PagoService pagoService;
	private final ConsultasApiService consultasApiService;

	public PagoApiController(PagoService pagoService,
			ConsultasApiService consultasApiService) {
		this.pagoService = pagoService;
		this.consultasApiService = consultasApiService;
	}

	@GetMapping("/reservas/{id}/saldo")
	public SaldoReservaDto obtenerSaldo(@PathVariable Integer id) {
		return pagoService.obtenerSaldo(id);
	}

	@GetMapping("/reservas/{id}/pagos")
	public List<PagoDto> listarPorReserva(@PathVariable Integer id) {
		pagoService.obtenerSaldo(id);
		return consultasApiService.listarPagosReserva(id);
	}

	@PostMapping("/reservas/{id}/pagos")
	public ResponseEntity<PagoDto> registrar(@PathVariable Integer id,
			@Valid @RequestBody PagoRequest solicitud) {
		if (solicitud.tipo() == com.hostops.hotelmanagement.models.TipoMovimientoPago.reembolso) {
			SeguridadUtils.exigirAdministrador();
		}
		Pago pago = pagoService.registrar(id, new PagoComando(
				solicitud.tipo(),
				solicitud.metodo(),
				solicitud.monto(),
				SeguridadUtils.idUsuarioActual(),
				solicitud.motivo()));
		return ResponseEntity.created(URI.create("/api/pagos/" + pago.getIdPago()))
				.body(consultasApiService.obtenerPago(pago.getIdPago()));
	}

	@GetMapping("/pagos/{id}")
	public PagoDto obtener(@PathVariable Integer id) {
		return consultasApiService.obtenerPago(id);
	}

	@GetMapping("/pagos")
	public List<PagoDto> listarRecientes(
			@RequestParam(defaultValue = "12") @Min(1) @Max(100) int limit) {
		return consultasApiService.listarPagosRecientes(limit);
	}

	@PostMapping("/pagos/{id}/compensacion")
	public ResponseEntity<PagoDto> compensar(@PathVariable Integer id,
			@Valid @RequestBody CompensacionPagoRequest solicitud) {
		SeguridadUtils.exigirAdministrador();
		Pago compensacion = pagoService.compensar(id, new CompensacionPagoComando(
				solicitud.metodo(),
				solicitud.monto(),
				SeguridadUtils.idUsuarioActual()));
		return ResponseEntity.created(
				URI.create("/api/pagos/" + compensacion.getIdPago()))
				.body(consultasApiService.obtenerPago(compensacion.getIdPago()));
	}

}
