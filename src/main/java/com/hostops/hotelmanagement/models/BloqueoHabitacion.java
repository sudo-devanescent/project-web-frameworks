package com.hostops.hotelmanagement.models;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bloqueos de habitación por rango de fechas. Refleja la tabla
 * {@code bloqueo_habitacion}. El EXCLUDE de solape y la FK los aplica la BD.
 */
@Entity
@Table(name = "bloqueo_habitacion")
public class BloqueoHabitacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_bloqueo", nullable = false)
	private Integer idBloqueo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_habitacion", nullable = false)
	private Habitacion habitacion;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;

	@Column(name = "motivo", nullable = false)
	private String motivo;

	public BloqueoHabitacion() {
	}

	public Integer getIdBloqueo() {
		return idBloqueo;
	}

	public void setIdBloqueo(Integer idBloqueo) {
		this.idBloqueo = idBloqueo;
	}

	public Habitacion getHabitacion() {
		return habitacion;
	}

	public void setHabitacion(Habitacion habitacion) {
		this.habitacion = habitacion;
	}

	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaInicio(LocalDate fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public LocalDate getFechaFin() {
		return fechaFin;
	}

	public void setFechaFin(LocalDate fechaFin) {
		this.fechaFin = fechaFin;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BloqueoHabitacion other)) {
			return false;
		}
		return idBloqueo != null && idBloqueo.equals(other.idBloqueo);
	}

	@Override
	public int hashCode() {
		return BloqueoHabitacion.class.hashCode();
	}
}
