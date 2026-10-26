package com.hostops.hotelmanagement.models;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Cancelación de un detalle de reserva (1 : 0..1). Refleja la tabla
 * {@code cancelacion}. La UNIQUE, la FK y el trigger que marca el detalle como
 * cancelada los aplica la base de datos. {@code fecha_cancelacion} y
 * {@code penalidad} tienen DEFAULT en BD: con {@code @DynamicInsert} se omiten
 * si quedan nulos.
 */
@Entity
@Table(name = "cancelacion")
@DynamicInsert
public class Cancelacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cancelacion", nullable = false)
	private Integer idCancelacion;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_detalle", nullable = false, unique = true)
	private DetalleReserva detalleReserva;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false)
	private Usuario usuario;

	@Column(name = "fecha_cancelacion", nullable = false)
	private Instant fechaCancelacion;

	@Column(name = "motivo", nullable = false)
	private String motivo;

	@Column(name = "penalidad", nullable = false, precision = 10, scale = 2)
	private BigDecimal penalidad;

	public Cancelacion() {
	}

	public Integer getIdCancelacion() {
		return idCancelacion;
	}

	public void setIdCancelacion(Integer idCancelacion) {
		this.idCancelacion = idCancelacion;
	}

	public DetalleReserva getDetalleReserva() {
		return detalleReserva;
	}

	public void setDetalleReserva(DetalleReserva detalleReserva) {
		this.detalleReserva = detalleReserva;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Instant getFechaCancelacion() {
		return fechaCancelacion;
	}

	public void setFechaCancelacion(Instant fechaCancelacion) {
		this.fechaCancelacion = fechaCancelacion;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public BigDecimal getPenalidad() {
		return penalidad;
	}

	public void setPenalidad(BigDecimal penalidad) {
		this.penalidad = penalidad;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Cancelacion other)) {
			return false;
		}
		return idCancelacion != null && idCancelacion.equals(other.idCancelacion);
	}

	@Override
	public int hashCode() {
		return Cancelacion.class.hashCode();
	}
}
