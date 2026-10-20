package com.hostops.hotelmanagement.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Detalle de cada habitación dentro de una reserva. Refleja la tabla
 * {@code detalle_reserva}. El EXCLUDE de solape, las FK y los triggers los
 * aplica la base de datos. {@code estado} tiene DEFAULT en BD: con
 * {@code @DynamicInsert} se omite si queda nulo.
 */
@Entity
@Table(name = "detalle_reserva")
@DynamicInsert
public class DetalleReserva {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_detalle", nullable = false)
	private Integer idDetalle;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_reserva", nullable = false)
	private Reserva reserva;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_habitacion", nullable = false)
	private Habitacion habitacion;

	@Column(name = "fecha_entrada_prevista", nullable = false)
	private LocalDate fechaEntradaPrevista;

	@Column(name = "fecha_salida_prevista", nullable = false)
	private LocalDate fechaSalidaPrevista;

	@Column(name = "checkin_real")
	private Instant checkinReal;

	@Column(name = "checkout_real")
	private Instant checkoutReal;

	@Column(name = "tarifa_noche", nullable = false, precision = 10, scale = 2)
	private BigDecimal tarifaNoche;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "estado", nullable = false, columnDefinition = "estado_detalle")
	private EstadoDetalle estado;

	public DetalleReserva() {
	}

	public Integer getIdDetalle() {
		return idDetalle;
	}

	public void setIdDetalle(Integer idDetalle) {
		this.idDetalle = idDetalle;
	}

	public Reserva getReserva() {
		return reserva;
	}

	public void setReserva(Reserva reserva) {
		this.reserva = reserva;
	}

	public Habitacion getHabitacion() {
		return habitacion;
	}

	public void setHabitacion(Habitacion habitacion) {
		this.habitacion = habitacion;
	}

	public LocalDate getFechaEntradaPrevista() {
		return fechaEntradaPrevista;
	}

	public void setFechaEntradaPrevista(LocalDate fechaEntradaPrevista) {
		this.fechaEntradaPrevista = fechaEntradaPrevista;
	}

	public LocalDate getFechaSalidaPrevista() {
		return fechaSalidaPrevista;
	}

	public void setFechaSalidaPrevista(LocalDate fechaSalidaPrevista) {
		this.fechaSalidaPrevista = fechaSalidaPrevista;
	}

	public Instant getCheckinReal() {
		return checkinReal;
	}

	public void setCheckinReal(Instant checkinReal) {
		this.checkinReal = checkinReal;
	}

	public Instant getCheckoutReal() {
		return checkoutReal;
	}

	public void setCheckoutReal(Instant checkoutReal) {
		this.checkoutReal = checkoutReal;
	}

	public BigDecimal getTarifaNoche() {
		return tarifaNoche;
	}

	public void setTarifaNoche(BigDecimal tarifaNoche) {
		this.tarifaNoche = tarifaNoche;
	}

	public EstadoDetalle getEstado() {
		return estado;
	}

	public void setEstado(EstadoDetalle estado) {
		this.estado = estado;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DetalleReserva other)) {
			return false;
		}
		return idDetalle != null && idDetalle.equals(other.idDetalle);
	}

	@Override
	public int hashCode() {
		return DetalleReserva.class.hashCode();
	}
}
