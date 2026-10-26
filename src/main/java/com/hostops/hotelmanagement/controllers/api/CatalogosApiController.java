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
import org.springframework.web.bind.annotation.RestController;

import com.hostops.hotelmanagement.controllers.api.dto.CanalRequest;
import com.hostops.hotelmanagement.models.Canal;
import com.hostops.hotelmanagement.services.CanalService;
import com.hostops.hotelmanagement.services.ConsultasApiService;
import com.hostops.hotelmanagement.services.dto.CanalDto;
import com.hostops.hotelmanagement.services.dto.RolDto;

import jakarta.validation.Valid;

/**
 * Expone los roles y canales de reserva.
 */
@RestController
@RequestMapping("/api")
@Validated
public class CatalogosApiController {

	private final CanalService canalService;
	private final ConsultasApiService consultasApiService;

	public CatalogosApiController(CanalService canalService,
			ConsultasApiService consultasApiService) {
		this.canalService = canalService;
		this.consultasApiService = consultasApiService;
	}

	@GetMapping("/roles")
	public List<RolDto> listarRoles() {
		return consultasApiService.listarRoles();
	}

	@GetMapping("/canales")
	public List<CanalDto> listarCanales() {
		return consultasApiService.listarCanales();
	}

	@GetMapping("/canales/{id}")
	public CanalDto obtenerCanal(@PathVariable Integer id) {
		return consultasApiService.obtenerCanal(id);
	}

	@PostMapping("/canales")
	public ResponseEntity<CanalDto> crearCanal(@Valid @RequestBody CanalRequest solicitud) {
		Canal canal = canalService.crear(solicitud.nombre());
		CanalDto creado = consultasApiService.obtenerCanal(canal.getIdCanal());
		return ResponseEntity.created(URI.create("/api/canales/" + canal.getIdCanal()))
				.body(creado);
	}

	@PutMapping("/canales/{id}")
	public CanalDto actualizarCanal(@PathVariable Integer id,
			@Valid @RequestBody CanalRequest solicitud) {
		Canal canal = canalService.actualizar(id, solicitud.nombre());
		return consultasApiService.obtenerCanal(canal.getIdCanal());
	}

}
