package com.hostops.hotelmanagement.controllers.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.hostops.hotelmanagement.services.exception.AutenticacionFallidaException;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.DatosInvalidosException;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.SaldoPendienteException;

import jakarta.validation.ConstraintViolationException;

/**
 * Convierte los errores de la API en respuestas ProblemDetail consistentes.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(AutenticacionFallidaException.class)
	public ProblemDetail autenticacionFallida(AutenticacionFallidaException excepcion) {
		return problema(HttpStatus.UNAUTHORIZED, "No autenticado", excepcion.getMessage());
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException excepcion) {
		return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", excepcion.getMessage());
	}

	@ExceptionHandler(SaldoPendienteException.class)
	public ProblemDetail saldoPendiente(SaldoPendienteException excepcion) {
		ProblemDetail detalle = problema(HttpStatus.CONFLICT, "Saldo pendiente",
				excepcion.getMessage());
		detalle.setProperty("saldo", excepcion.getSaldo());
		return detalle;
	}

	@ExceptionHandler(ConflictoException.class)
	public ProblemDetail conflicto(ConflictoException excepcion) {
		return problema(HttpStatus.CONFLICT, "Conflicto", excepcion.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ProblemDetail reglaNegocio(ReglaNegocioException excepcion) {
		return problema(HttpStatus.CONFLICT, "Regla de negocio", excepcion.getMessage());
	}

	@ExceptionHandler({DatosInvalidosException.class, ConstraintViolationException.class})
	public ProblemDetail datosInvalidos(RuntimeException excepcion) {
		return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", excepcion.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail validacion(MethodArgumentNotValidException excepcion) {
		Map<String, String> errores = new LinkedHashMap<>();
		excepcion.getBindingResult().getFieldErrors().forEach(error ->
				errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail detalle = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
				"Revisa los campos enviados.");
		detalle.setProperty("errores", errores);
		return detalle;
	}

	@ExceptionHandler({HttpMessageNotReadableException.class,
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class})
	public ProblemDetail solicitudInvalida(Exception excepcion) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"El cuerpo o los parámetros de la solicitud no son válidos.");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ProblemDetail tipoContenidoNoSoportado(HttpMediaTypeNotSupportedException excepcion) {
		return problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado",
				"El tipo de contenido de la solicitud no está soportado.");
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail validacionDeParametros(HandlerMethodValidationException excepcion) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"Los parámetros de la solicitud no son válidos.");
	}

	@ExceptionHandler({NoResourceFoundException.class})
	public ProblemDetail recursoInexistente(Exception excepcion) {
		return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado",
				"El recurso solicitado no existe.");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail metodoNoSoportado(HttpRequestMethodNotSupportedException excepcion) {
		return problema(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
				"El método HTTP no está permitido para este recurso.");
	}

	@ExceptionHandler(PessimisticLockingFailureException.class)
	public ResponseEntity<ProblemDetail> bloqueoNoDisponible(
			PessimisticLockingFailureException excepcion) {
		log.warn("No se pudo adquirir el bloqueo de la operación", excepcion);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.header("Retry-After", "1")
				.body(problema(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible",
						"La operación está siendo modificada por otra petición. Reintenta."));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail accesoDenegado(AccessDeniedException excepcion) {
		return problema(HttpStatus.FORBIDDEN, "Acceso denegado", excepcion.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail integridad(DataIntegrityViolationException excepcion) {
		log.warn("Violación de integridad no traducida por el servicio", excepcion);
		return problema(HttpStatus.CONFLICT, "Conflicto",
				"La operación entra en conflicto con los datos existentes.");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail errorInesperado(Exception excepcion) {
		log.error("Error no controlado en la API", excepcion);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
				"No se pudo completar la operación.");
	}

	private ProblemDetail problema(HttpStatus estado, String titulo, String mensaje) {
		ProblemDetail detalle = ProblemDetail.forStatusAndDetail(estado, mensaje);
		detalle.setTitle(titulo);
		return detalle;
	}

}
