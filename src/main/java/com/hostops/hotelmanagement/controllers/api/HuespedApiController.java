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

import com.hostops.hotelmanagement.controllers.api.dto.HuespedRequest;
import com.hostops.hotelmanagement.models.Huesped;
import com.hostops.hotelmanagement.services.ConsultasApiService;
import com.hostops.hotelmanagement.services.HuespedService;
import com.hostops.hotelmanagement.services.dto.EstadiaDto;
import com.hostops.hotelmanagement.services.dto.HuespedComando;
import com.hostops.hotelmanagement.services.dto.HuespedDto;

import jakarta.validation.Valid;

/**
 * Expone la consulta y mantenimiento de huéspedes.
 */
@RestController
@RequestMapping("/api/huespedes")
@Validated
public class HuespedApiController {

	private final HuespedService huespedService;
	private final ConsultasApiService consultasApiService;

	public HuespedApiController(HuespedService huespedService,
			ConsultasApiService consultasApiService) {
		this.huespedService = huespedService;
		this.consultasApiService = consultasApiService;
	}

	@GetMapping
	public List<HuespedDto> listar(@RequestParam(required = false) String q) {
		return consultasApiService.listarHuespedes(q);
	}

	@GetMapping("/{id}")
	public HuespedDto obtener(@PathVariable Integer id) {
		return consultasApiService.obtenerHuesped(id);
	}

	@GetMapping("/{id}/estadias")
	public List<EstadiaDto> listarEstadias(@PathVariable Integer id) {
		consultasApiService.obtenerHuesped(id);
		return consultasApiService.listarEstadiasHuesped(id);
	}

	@PostMapping
	public ResponseEntity<HuespedDto> crear(@Valid @RequestBody HuespedRequest solicitud) {
		Huesped huesped = huespedService.crear(comando(solicitud));
		return ResponseEntity.created(
				URI.create("/api/huespedes/" + huesped.getIdHuesped()))
				.body(consultasApiService.obtenerHuesped(huesped.getIdHuesped()));
	}

	@PutMapping("/{id}")
	public HuespedDto actualizar(@PathVariable Integer id,
			@Valid @RequestBody HuespedRequest solicitud) {
		Huesped huesped = huespedService.actualizar(id, comando(solicitud));
		return consultasApiService.obtenerHuesped(huesped.getIdHuesped());
	}

	private HuespedComando comando(HuespedRequest solicitud) {
		return new HuespedComando(
				solicitud.tipoDocumento(),
				solicitud.numeroDocumento(),
				solicitud.nombres(),
				solicitud.apellidos(),
				solicitud.telefono(),
				solicitud.correo(),
				solicitud.nacionalidad(),
				solicitud.fechaNacimiento(),
				solicitud.preferencias());
	}

}
