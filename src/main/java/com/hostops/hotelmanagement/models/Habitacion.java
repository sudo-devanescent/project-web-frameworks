package com.hostops.hotelmanagement.models;

import java.math.BigDecimal;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Habitaciones del hotel. Refleja la tabla {@code habitacion}.
 * El CHECK de capacidad, las restricciones UNIQUE y los triggers los aplica la
 * base de datos. {@code estado} tiene DEFAULT en BD: con {@code @DynamicInsert}
 * se omite si queda nulo y lo rellena la BD.
 */
@Entity
@Table(name = "habitacion")
@DynamicInsert
public class Habitacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_habitacion", nullable = false)
	private Integer idHabitacion;

	@Column(name = "numero", nullable = false, length = 10, unique = true)
	private String numero;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "tipo", nullable = false, columnDefinition = "tipo_habitacion")
	private TipoHabitacion tipo;

	@Column(name = "capacidad", nullable = false)
	private Short capacidad;

	@Column(name = "tarifa", nullable = false, precision = 10, scale = 2)
	private BigDecimal tarifa;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "estado", nullable = false, columnDefinition = "estado_habitacion")
	private EstadoHabitacion estado;

	public Habitacion() {
	}

	public Integer getIdHabitacion() {
		return idHabitacion;
	}

	public void setIdHabitacion(Integer idHabitacion) {
		this.idHabitacion = idHabitacion;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public TipoHabitacion getTipo() {
		return tipo;
	}

	public void setTipo(TipoHabitacion tipo) {
		this.tipo = tipo;
	}

	public Short getCapacidad() {
		return capacidad;
	}

	public void setCapacidad(Short capacidad) {
		this.capacidad = capacidad;
	}

	public BigDecimal getTarifa() {
		return tarifa;
	}

	public void setTarifa(BigDecimal tarifa) {
		this.tarifa = tarifa;
	}

	public EstadoHabitacion getEstado() {
		return estado;
	}

	public void setEstado(EstadoHabitacion estado) {
		this.estado = estado;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Habitacion other)) {
			return false;
		}
		return idHabitacion != null && idHabitacion.equals(other.idHabitacion);
	}

	@Override
	public int hashCode() {
		return Habitacion.class.hashCode();
	}
}
