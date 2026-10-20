package com.hostops.hotelmanagement.models;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Clave compuesta de {@link DetalleHuesped} (id_detalle, id_huesped).
 */
@Embeddable
public class DetalleHuespedId implements Serializable {

	@Column(name = "id_detalle", nullable = false)
	private Integer idDetalle;

	@Column(name = "id_huesped", nullable = false)
	private Integer idHuesped;

	public DetalleHuespedId() {
	}

	public DetalleHuespedId(Integer idDetalle, Integer idHuesped) {
		this.idDetalle = idDetalle;
		this.idHuesped = idHuesped;
	}

	public Integer getIdDetalle() {
		return idDetalle;
	}

	public void setIdDetalle(Integer idDetalle) {
		this.idDetalle = idDetalle;
	}

	public Integer getIdHuesped() {
		return idHuesped;
	}

	public void setIdHuesped(Integer idHuesped) {
		this.idHuesped = idHuesped;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DetalleHuespedId other)) {
			return false;
		}
		return Objects.equals(idDetalle, other.idDetalle)
				&& Objects.equals(idHuesped, other.idHuesped);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idDetalle, idHuesped);
	}
}
