package com.hostops.hotelmanagement.services;

import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Rol;
import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.repositories.RolRepository;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.security.SesionRevocadaEvent;
import com.hostops.hotelmanagement.services.dto.UsuarioComando;
import com.hostops.hotelmanagement.services.dto.UsuarioDto;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class UsuarioService {

	private static final String ROL_ADMINISTRADOR = "administrador";

	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final ReservaRepository reservaRepository;
	private final TraductorErroresIntegridad traductor;
	private final ApplicationEventPublisher eventPublisher;
	private final EntityManager entityManager;

	public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
			ReservaRepository reservaRepository, TraductorErroresIntegridad traductor,
			ApplicationEventPublisher eventPublisher, EntityManager entityManager) {
		this.usuarioRepository = usuarioRepository;
		this.rolRepository = rolRepository;
		this.reservaRepository = reservaRepository;
		this.traductor = traductor;
		this.eventPublisher = eventPublisher;
		this.entityManager = entityManager;
	}

	public List<UsuarioDto> listar() {
		return usuarioRepository.findAll(Sort.by("nombreUsuario")).stream()
				.map(UsuarioDto::desde)
				.toList();
	}

	public UsuarioDto obtener(Integer idUsuario) {
		Usuario usuario = usuarioRepository.findById(idUsuario)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario indicado."));
		return UsuarioDto.desde(usuario);
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public UsuarioDto crear(UsuarioComando comando) {
		validarComando(comando, true);
		Rol rol = obtenerRol(comando.idRol());
		String nombreUsuario = comando.nombreUsuario().trim();
		String correo = comando.correo().trim();
		if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
			throw new ConflictoException("Ya existe un usuario con ese nombre de usuario.");
		}
		if (usuarioRepository.existsByCorreo(correo)) {
			throw new ConflictoException("Ya existe un usuario con ese correo.");
		}

		Usuario usuario = new Usuario();
		usuario.setRol(rol);
		usuario.setNombreUsuario(nombreUsuario);
		usuario.setNombreCompleto(comando.nombreCompleto().trim());
		usuario.setCorreo(correo);
		usuario.setContrasenaHash(comando.contrasenaHash());
		usuario.setActivo(comando.activo());
		try {
			Usuario guardado = usuarioRepository.saveAndFlush(usuario);
			entityManager.refresh(guardado);
			return UsuarioDto.desde(guardado);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public UsuarioDto actualizar(Integer idUsuario, UsuarioComando comando) {
		bloquearAdministradoresActivos();
		Usuario usuario = usuarioRepository.findByIdForUpdate(idUsuario)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario indicado."));
		validarComando(comando, false);
		Rol rol = obtenerRol(comando.idRol());
		String nombreUsuario = comando.nombreUsuario().trim();
		String correo = comando.correo().trim();
		if (usuarioRepository.existsByNombreUsuarioAndIdUsuarioNot(nombreUsuario, idUsuario)) {
			throw new ConflictoException("Ya existe un usuario con ese nombre de usuario.");
		}
		if (usuarioRepository.existsByCorreoAndIdUsuarioNot(correo, idUsuario)) {
			throw new ConflictoException("Ya existe un usuario con ese correo.");
		}

		boolean eraAdministradorActivo = esAdministradorActivo(usuario);
		Integer idRolAnterior = usuario.getRol().getIdRol();
		String hashAnterior = usuario.getContrasenaHash();
		Boolean activoAnterior = usuario.getActivo();
		Boolean activo = comando.activo() != null ? comando.activo() : usuario.getActivo();
		boolean seraAdministradorActivo = Boolean.TRUE.equals(activo) && esRolAdministrador(rol);
		if (eraAdministradorActivo && !seraAdministradorActivo
				&& usuarioRepository.countByRol_NombreAndActivoTrue(ROL_ADMINISTRADOR) <= 1) {
			throw new ReglaNegocioException("Debe existir al menos un administrador activo.");
		}

		usuario.setRol(rol);
		usuario.setNombreUsuario(nombreUsuario);
		usuario.setNombreCompleto(comando.nombreCompleto().trim());
		usuario.setCorreo(correo);
		if (comando.contrasenaHash() != null && !comando.contrasenaHash().isBlank()) {
			usuario.setContrasenaHash(comando.contrasenaHash());
		}
		usuario.setActivo(activo);
		boolean contrasenaCambiada = comando.contrasenaHash() != null
				&& !comando.contrasenaHash().isBlank()
				&& !Objects.equals(hashAnterior, comando.contrasenaHash());
		boolean debeRevocar = !Objects.equals(idRolAnterior, rol.getIdRol())
				|| contrasenaCambiada
				|| (!Boolean.TRUE.equals(activo) && !Objects.equals(activoAnterior, activo));
		try {
			Usuario guardado = usuarioRepository.saveAndFlush(usuario);
			entityManager.refresh(guardado);
			if (debeRevocar) {
				eventPublisher.publishEvent(new SesionRevocadaEvent(idUsuario));
			}
			return UsuarioDto.desde(guardado);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public UsuarioDto cambiarActivo(Integer idUsuario, Boolean activo) {
		if (activo == null) {
			throw new DatosInvalidosException("El estado activo es obligatorio.");
		}
		bloquearAdministradoresActivos();
		Usuario usuario = usuarioRepository.findByIdForUpdate(idUsuario)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario indicado."));
		if (esAdministradorActivo(usuario) && !activo
				&& usuarioRepository.countByRol_NombreAndActivoTrue(ROL_ADMINISTRADOR) <= 1) {
			throw new ReglaNegocioException("Debe existir al menos un administrador activo.");
		}

		usuario.setActivo(activo);
		try {
			Usuario guardado = usuarioRepository.saveAndFlush(usuario);
			entityManager.refresh(guardado);
			if (!Boolean.TRUE.equals(activo)) {
				eventPublisher.publishEvent(new SesionRevocadaEvent(idUsuario));
			}
			return UsuarioDto.desde(guardado);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idUsuario) {
		bloquearAdministradoresActivos();
		Usuario usuario = usuarioRepository.findByIdForUpdate(idUsuario)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario indicado."));
		if (esAdministradorActivo(usuario)
				&& usuarioRepository.countByRol_NombreAndActivoTrue(ROL_ADMINISTRADOR) <= 1) {
			throw new ReglaNegocioException("Debe existir al menos un administrador activo.");
		}
		if (reservaRepository.existsByUsuarioCreador_IdUsuario(idUsuario)
				|| reservaRepository.existsByUsuarioModificador_IdUsuario(idUsuario)) {
			throw new ConflictoException(
					"El usuario tiene reservas asociadas y no se puede eliminar.");
		}

		try {
			usuarioRepository.delete(usuario);
			usuarioRepository.flush();
			eventPublisher.publishEvent(new SesionRevocadaEvent(idUsuario));
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private void bloquearAdministradoresActivos() {
		usuarioRepository.findActivosByRolNombreForUpdate(ROL_ADMINISTRADOR);
	}

	private Rol obtenerRol(Integer idRol) {
		return rolRepository.findById(idRol)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol indicado."));
	}

	private boolean esAdministradorActivo(Usuario usuario) {
		return Boolean.TRUE.equals(usuario.getActivo()) && esRolAdministrador(usuario.getRol());
	}

	private boolean esRolAdministrador(Rol rol) {
		return rol != null && ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());
	}

	private void validarComando(UsuarioComando comando, boolean creacion) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos del usuario son obligatorios.");
		}
		if (comando.idRol() == null) {
			throw new DatosInvalidosException("El rol del usuario es obligatorio.");
		}
		validarTextoObligatorio(comando.nombreUsuario(), "El nombre de usuario es obligatorio.", 50);
		validarTextoObligatorio(comando.nombreCompleto(), "El nombre completo es obligatorio.", 120);
		validarTextoObligatorio(comando.correo(), "El correo es obligatorio.", 120);
		if (creacion) {
			validarTextoObligatorio(comando.contrasenaHash(),
					"El hash de la contraseña es obligatorio.", 255);
		} else if (comando.contrasenaHash() != null
				&& comando.contrasenaHash().trim().length() > 255) {
			throw new DatosInvalidosException(
					"El hash de la contraseña no puede superar 255 caracteres.");
		}
	}

	private void validarTextoObligatorio(String valor, String mensaje, int longitudMaxima) {
		if (valor == null || valor.isBlank()) {
			throw new DatosInvalidosException(mensaje);
		}
		if (valor.trim().length() > longitudMaxima) {
			throw new DatosInvalidosException(mensaje.replace("es obligatorio",
					"no puede superar " + longitudMaxima + " caracteres"));
		}
	}

}
