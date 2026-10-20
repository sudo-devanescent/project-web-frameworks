package com.hostops.hotelmanagement.models;

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
import jakarta.persistence.Table;

/**
 * Reservas del hotel. Refleja la tabla {@code reserva}.
 * Las FK las aplica la base de datos. {@code fecha_creacion} tiene DEFAULT en BD
 * (con {@code @DynamicInsert} se omite si queda nula); {@code fecha_modificacion}
 * la escribe un trigger, por lo que no es insertable ni actualizable.
 */
@Entity
@Table(name = "reserva")
@DynamicInsert
public class Reserva {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_reserva", nullable = false)
	private Integer idReserva;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_huesped_titular", nullable = false)
	private Huesped huespedTitular;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_canal", nullable = false)
	private Canal canal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario_creador", nullable = false)
	private Usuario usuarioCreador;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario_modificador")
	private Usuario usuarioModificador;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_modificacion", insertable = false, updatable = false)
	private Instant fechaModificacion;

	public Reserva() {
	}

	public Integer getIdReserva() {
		return idReserva;
	}

	public void setIdReserva(Integer idReserva) {
		this.idReserva = idReserva;
	}

	public Huesped getHuespedTitular() {
		return huespedTitular;
	}

	public void setHuespedTitular(Huesped huespedTitular) {
		this.huespedTitular = huespedTitular;
	}

	public Canal getCanal() {
		return canal;
	}

	public void setCanal(Canal canal) {
		this.canal = canal;
	}

	public Usuario getUsuarioCreador() {
		return usuarioCreador;
	}

	public void setUsuarioCreador(Usuario usuarioCreador) {
		this.usuarioCreador = usuarioCreador;
	}

	public Usuario getUsuarioModificador() {
		return usuarioModificador;
	}

	public void setUsuarioModificador(Usuario usuarioModificador) {
		this.usuarioModificador = usuarioModificador;
	}

	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(Instant fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public Instant getFechaModificacion() {
		return fechaModificacion;
	}

	public void setFechaModificacion(Instant fechaModificacion) {
		this.fechaModificacion = fechaModificacion;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Reserva other)) {
			return false;
		}
		return idReserva != null && idReserva.equals(other.idReserva);
	}

	@Override
	public int hashCode() {
		return Reserva.class.hashCode();
	}
}
