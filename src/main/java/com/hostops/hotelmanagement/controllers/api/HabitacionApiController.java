package com.hostops.hotelmanagement.controllers.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostops.hotelmanagement.controllers.api.dto.BloqueoHabitacionRequest;
import com.hostops.hotelmanagement.controllers.api.dto.CambioEstadoHabitacionRequest;
import com.hostops.hotelmanagement.controllers.api.dto.HabitacionRequest;
import com.hostops.hotelmanagement.models.BloqueoHabitacion;
import com.hostops.hotelmanagement.models.Habitacion;
import com.hostops.hotelmanagement.services.BloqueoHabitacionService;
import com.hostops.hotelmanagement.services.ConsultasApiService;
import com.hostops.hotelmanagement.services.HabitacionService;
import com.hostops.hotelmanagement.services.dto.BloqueoHabitacionComando;
import com.hostops.hotelmanagement.services.dto.BloqueoHabitacionDto;
import com.hostops.hotelmanagement.services.dto.HabitacionComando;
import com.hostops.hotelmanagement.services.dto.HabitacionDto;

import jakarta.validation.Valid;

/**
 * Expone habitaciones y sus bloqueos operativos.
 */
@RestController
@RequestMapping("/api")
@Validated
public class HabitacionApiController {

	private final HabitacionService habitacionService;
	private final BloqueoHabitacionService bloqueoHabitacionService;
	private final ConsultasApiService consultasApiService;

	public HabitacionApiController(HabitacionService habitacionService,
			BloqueoHabitacionService bloqueoHabitacionService,
			ConsultasApiService consultasApiService) {
		this.habitacionService = habitacionService;
		this.bloqueoHabitacionService = bloqueoHabitacionService;
		this.consultasApiService = consultasApiService;
	}

	@GetMapping("/habitaciones")
	public List<HabitacionDto> listar() {
		return consultasApiService.listarHabitaciones();
	}

	@GetMapping("/habitaciones/{id}")
	public HabitacionDto obtener(@PathVariable Integer id) {
		return HabitacionDto.desde(habitacionService.obtener(id));
	}

	@PostMapping("/habitaciones")
	public ResponseEntity<HabitacionDto> crear(
			@Valid @RequestBody HabitacionRequest solicitud) {
		Habitacion habitacion = habitacionService.crear(comando(solicitud));
		return ResponseEntity.created(
				URI.create("/api/habitaciones/" + habitacion.getIdHabitacion()))
				.body(HabitacionDto.desde(habitacion));
	}

	@PutMapping("/habitaciones/{id}")
	public HabitacionDto actualizar(@PathVariable Integer id,
			@Valid @RequestBody HabitacionRequest solicitud) {
		return HabitacionDto.desde(habitacionService.actualizar(id, comando(solicitud)));
	}

	@PatchMapping("/habitaciones/{id}/estado")
	public HabitacionDto cambiarEstado(@PathVariable Integer id,
			@Valid @RequestBody CambioEstadoHabitacionRequest solicitud) {
		return HabitacionDto.desde(
				habitacionService.cambiarEstado(id, solicitud.estado()));
	}

	@GetMapping("/habitaciones/{id}/bloqueos")
	public List<BloqueoHabitacionDto> listarBloqueos(@PathVariable Integer id) {
		habitacionService.obtener(id);
		return consultasApiService.listarBloqueos(id);
	}

	@PostMapping("/habitaciones/{id}/bloqueos")
	public ResponseEntity<BloqueoHabitacionDto> crearBloqueo(@PathVariable Integer id,
			@Valid @RequestBody BloqueoHabitacionRequest solicitud) {
		BloqueoHabitacion bloqueo = bloqueoHabitacionService.crear(
				new BloqueoHabitacionComando(
						id,
						solicitud.fechaInicio(),
						solicitud.fechaFin(),
						solicitud.motivo()));
		return ResponseEntity.created(
				URI.create("/api/bloqueos/" + bloqueo.getIdBloqueo()))
				.body(consultasApiService.obtenerBloqueo(bloqueo.getIdBloqueo()));
	}

	@GetMapping("/bloqueos/{id}")
	public BloqueoHabitacionDto obtenerBloqueo(@PathVariable Integer id) {
		return consultasApiService.obtenerBloqueo(id);
	}

	@DeleteMapping("/bloqueos/{id}")
	public ResponseEntity<Void> eliminarBloqueo(@PathVariable Integer id) {
		bloqueoHabitacionService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	private HabitacionComando comando(HabitacionRequest solicitud) {
		return new HabitacionComando(
				solicitud.numero(),
				solicitud.tipo(),
				solicitud.capacidad(),
				solicitud.tarifa(),
				solicitud.estado());
	}

}
