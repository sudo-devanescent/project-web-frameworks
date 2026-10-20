package com.hostops.hotelmanagement.models;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Relación N:M entre detalle de reserva y huésped (huéspedes alojados en cada
 * habitación reservada). Refleja la tabla {@code detalle_huesped} con clave
 * primaria compuesta. Las FK las aplica la base de datos.
 */
@Entity
@Table(name = "detalle_huesped")
public class DetalleHuesped {

	@EmbeddedId
	private DetalleHuespedId id;

	@MapsId("idDetalle")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_detalle", nullable = false)
	private DetalleReserva detalleReserva;

	@MapsId("idHuesped")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_huesped", nullable = false)
	private Huesped huesped;

	public DetalleHuesped() {
	}

	public DetalleHuespedId getId() {
		return id;
	}

	public void setId(DetalleHuespedId id) {
		this.id = id;
	}

	public DetalleReserva getDetalleReserva() {
		return detalleReserva;
	}

	public void setDetalleReserva(DetalleReserva detalleReserva) {
		this.detalleReserva = detalleReserva;
	}

	public Huesped getHuesped() {
		return huesped;
	}

	public void setHuesped(Huesped huesped) {
		this.huesped = huesped;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DetalleHuesped other)) {
			return false;
		}
		return id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return DetalleHuesped.class.hashCode();
	}
}
