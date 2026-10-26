package com.hostops.hotelmanagement.services.exception;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Traduce las violaciones de integridad de PostgreSQL a excepciones de dominio.
 */
@Component
public class TraductorErroresIntegridad {

	private static final String UNICIDAD = "23505";
	private static final String CLAVE_FORANEA = "23503";
	private static final String CHECK = "23514";
	private static final String SOLAPAMIENTO = "23P01";
	private static final String RESTRICCION = "23001";

	public HostOpsException traducir(DataIntegrityViolationException excepcion) {
		String estado = obtenerEstadoSql(excepcion);

		if (UNICIDAD.equals(estado)) {
			return new ConflictoException(
					"Ya existe un registro con los mismos datos únicos.", excepcion);
		}
		if (CLAVE_FORANEA.equals(estado)) {
			return new ConflictoException(
					"La operación entra en conflicto con registros relacionados.", excepcion);
		}
		if (CHECK.equals(estado)) {
			return new ReglaNegocioException(
					"Los datos no cumplen una restricción de la base de datos.", excepcion);
		}
		if (SOLAPAMIENTO.equals(estado)) {
			return new ConflictoException(
					"La operación solapa con un registro existente.", excepcion);
		}
		if (RESTRICCION.equals(estado)) {
			return new ReglaNegocioException(
					"La operación no se puede completar por una restricción de integridad.",
					excepcion);
		}
		return new ConflictoException(
				"Los datos violan una restricción de integridad.", excepcion);
	}

	private String obtenerEstadoSql(Throwable error) {
		Throwable actual = error;
		while (actual != null) {
			if (actual instanceof SQLException sqlException
					&& sqlException.getSQLState() != null) {
				return sqlException.getSQLState();
			}
			actual = actual.getCause();
		}
		return null;
	}

}
