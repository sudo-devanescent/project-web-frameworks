package com.hostops.hotelmanagement.services;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Canal;
import com.hostops.hotelmanagement.repositories.CanalRepository;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class CanalService {

	private final CanalRepository canalRepository;
	private final ReservaRepository reservaRepository;
	private final TraductorErroresIntegridad traductor;

	public CanalService(CanalRepository canalRepository, ReservaRepository reservaRepository,
			TraductorErroresIntegridad traductor) {
		this.canalRepository = canalRepository;
		this.reservaRepository = reservaRepository;
		this.traductor = traductor;
	}

	public List<Canal> listar() {
		return canalRepository.findAll(Sort.by("nombre"));
	}

	public Canal obtener(Integer idCanal) {
		return canalRepository.findById(idCanal)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el canal indicado."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Canal crear(String nombre) {
		String nombreValido = validarNombre(nombre);
		if (canalRepository.existsByNombre(nombreValido)) {
			throw new ConflictoException("Ya existe un canal con ese nombre.");
		}

		Canal canal = new Canal();
		canal.setNombre(nombreValido);
		try {
			return canalRepository.saveAndFlush(canal);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Canal actualizar(Integer idCanal, String nombre) {
		Canal canal = obtener(idCanal);
		String nombreValido = validarNombre(nombre);
		if (canalRepository.existsByNombreAndIdCanalNot(nombreValido, idCanal)) {
			throw new ConflictoException("Ya existe un canal con ese nombre.");
		}

		canal.setNombre(nombreValido);
		try {
			return canalRepository.saveAndFlush(canal);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idCanal) {
		Canal canal = obtener(idCanal);
		if (reservaRepository.existsByCanal_IdCanal(idCanal)) {
			throw new ConflictoException("El canal tiene reservas asociadas y no se puede eliminar.");
		}

		try {
			canalRepository.delete(canal);
			canalRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private String validarNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new DatosInvalidosException("El nombre del canal es obligatorio.");
		}
		String valor = nombre.trim();
		if (valor.length() > 50) {
			throw new DatosInvalidosException("El nombre del canal no puede superar 50 caracteres.");
		}
		return valor;
	}

}
