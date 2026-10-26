package com.hostops.hotelmanagement.services;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Rol;
import com.hostops.hotelmanagement.repositories.RolRepository;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class RolService {

	private final RolRepository rolRepository;
	private final UsuarioRepository usuarioRepository;
	private final TraductorErroresIntegridad traductor;

	public RolService(RolRepository rolRepository, UsuarioRepository usuarioRepository,
			TraductorErroresIntegridad traductor) {
		this.rolRepository = rolRepository;
		this.usuarioRepository = usuarioRepository;
		this.traductor = traductor;
	}

	public List<Rol> listar() {
		return rolRepository.findAll(Sort.by("nombre"));
	}

	public Rol obtener(Integer idRol) {
		return rolRepository.findById(idRol)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol indicado."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Rol crear(String nombre) {
		String nombreValido = validarNombre(nombre);
		if (rolRepository.existsByNombre(nombreValido)) {
			throw new ConflictoException("Ya existe un rol con ese nombre.");
		}

		Rol rol = new Rol();
		rol.setNombre(nombreValido);
		try {
			return rolRepository.saveAndFlush(rol);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Rol actualizar(Integer idRol, String nombre) {
		Rol rol = obtener(idRol);
		String nombreValido = validarNombre(nombre);
		if (rolRepository.existsByNombreAndIdRolNot(nombreValido, idRol)) {
			throw new ConflictoException("Ya existe un rol con ese nombre.");
		}

		rol.setNombre(nombreValido);
		try {
			return rolRepository.saveAndFlush(rol);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idRol) {
		Rol rol = obtener(idRol);
		if (usuarioRepository.existsByRol_IdRol(idRol)) {
			throw new ConflictoException("El rol tiene usuarios asociados y no se puede eliminar.");
		}

		try {
			rolRepository.delete(rol);
			rolRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private String validarNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new DatosInvalidosException("El nombre del rol es obligatorio.");
		}
		String valor = nombre.trim();
		if (valor.length() > 50) {
			throw new DatosInvalidosException("El nombre del rol no puede superar 50 caracteres.");
		}
		return valor;
	}

}
