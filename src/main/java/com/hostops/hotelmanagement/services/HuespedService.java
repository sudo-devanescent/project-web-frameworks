package com.hostops.hotelmanagement.services;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Huesped;
import com.hostops.hotelmanagement.repositories.DetalleHuespedRepository;
import com.hostops.hotelmanagement.repositories.HuespedRepository;
import com.hostops.hotelmanagement.repositories.ReservaRepository;
import com.hostops.hotelmanagement.services.dto.HuespedComando;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.TraductorErroresIntegridad;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class HuespedService {

	private final HuespedRepository huespedRepository;
	private final ReservaRepository reservaRepository;
	private final DetalleHuespedRepository detalleHuespedRepository;
	private final TraductorErroresIntegridad traductor;

	public HuespedService(HuespedRepository huespedRepository, ReservaRepository reservaRepository,
			DetalleHuespedRepository detalleHuespedRepository,
			TraductorErroresIntegridad traductor) {
		this.huespedRepository = huespedRepository;
		this.reservaRepository = reservaRepository;
		this.detalleHuespedRepository = detalleHuespedRepository;
		this.traductor = traductor;
	}

	public List<Huesped> listar() {
		return huespedRepository.findAll(Sort.by("apellidos", "nombres"));
	}

	public List<Huesped> buscar(String texto) {
		if (texto == null || texto.isBlank()) {
			return listar();
		}
		String filtro = texto.trim();
		return huespedRepository
				.findByApellidosContainingIgnoreCaseOrNombresContainingIgnoreCaseOrderByApellidosAscNombresAsc(
						filtro, filtro);
	}

	public Huesped obtener(Integer idHuesped) {
		return huespedRepository.findById(idHuesped)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el huésped indicado."));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Huesped crear(HuespedComando comando) {
		validarComando(comando);
		String tipoDocumento = comando.tipoDocumento().trim();
		String numeroDocumento = comando.numeroDocumento().trim();
		if (huespedRepository.existsByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)) {
			throw new ConflictoException("Ya existe un huésped con ese documento.");
		}

		Huesped huesped = new Huesped();
		aplicar(huesped, comando);
		try {
			return huespedRepository.saveAndFlush(huesped);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Huesped actualizar(Integer idHuesped, HuespedComando comando) {
		Huesped huesped = obtener(idHuesped);
		validarComando(comando);
		String tipoDocumento = comando.tipoDocumento().trim();
		String numeroDocumento = comando.numeroDocumento().trim();
		if (huespedRepository.existsByTipoDocumentoAndNumeroDocumentoAndIdHuespedNot(
				tipoDocumento, numeroDocumento, idHuesped)) {
			throw new ConflictoException("Ya existe un huésped con ese documento.");
		}

		aplicar(huesped, comando);
		try {
			return huespedRepository.saveAndFlush(huesped);
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void eliminar(Integer idHuesped) {
		Huesped huesped = obtener(idHuesped);
		if (reservaRepository.existsByHuespedTitular_IdHuesped(idHuesped)
				|| detalleHuespedRepository.existsByHuesped_IdHuesped(idHuesped)) {
			throw new ConflictoException(
					"El huésped tiene reservas o alojamientos asociados y no se puede eliminar.");
		}

		try {
			huespedRepository.delete(huesped);
			huespedRepository.flush();
		} catch (DataIntegrityViolationException excepcion) {
			throw traductor.traducir(excepcion);
		}
	}

	private void validarComando(HuespedComando comando) {
		if (comando == null) {
			throw new DatosInvalidosException("Los datos del huésped son obligatorios.");
		}
		validarTextoObligatorio(comando.tipoDocumento(), "El tipo de documento es obligatorio.", 20);
		validarTextoObligatorio(comando.numeroDocumento(), "El número de documento es obligatorio.", 30);
		validarTextoObligatorio(comando.nombres(), "Los nombres son obligatorios.", 100);
		validarTextoObligatorio(comando.apellidos(), "Los apellidos son obligatorios.", 100);
		validarTextoObligatorio(comando.telefono(), "El teléfono es obligatorio.", 30);
		validarTextoObligatorio(comando.nacionalidad(), "La nacionalidad es obligatoria.", 60);
		if (comando.correo() != null && comando.correo().trim().length() > 120) {
			throw new DatosInvalidosException("El correo no puede superar 120 caracteres.");
		}
		if (comando.fechaNacimiento() == null) {
			throw new DatosInvalidosException("La fecha de nacimiento es obligatoria.");
		}
	}

	private void validarTextoObligatorio(String valor, String mensaje, int longitudMaxima) {
		if (valor == null || valor.isBlank()) {
			throw new DatosInvalidosException(mensaje);
		}
		if (valor.trim().length() > longitudMaxima) {
			throw new DatosInvalidosException(mensaje.replace("es obligatorio", "no puede superar "
					+ longitudMaxima + " caracteres"));
		}
	}

	private void aplicar(Huesped huesped, HuespedComando comando) {
		huesped.setTipoDocumento(comando.tipoDocumento().trim());
		huesped.setNumeroDocumento(comando.numeroDocumento().trim());
		huesped.setNombres(comando.nombres().trim());
		huesped.setApellidos(comando.apellidos().trim());
		huesped.setTelefono(comando.telefono().trim());
		huesped.setCorreo(normalizarOpcional(comando.correo()));
		huesped.setNacionalidad(comando.nacionalidad().trim());
		huesped.setFechaNacimiento(comando.fechaNacimiento());
		huesped.setPreferencias(normalizarOpcional(comando.preferencias()));
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

}
