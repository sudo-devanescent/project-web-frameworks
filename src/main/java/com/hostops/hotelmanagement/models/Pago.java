package com.hostops.hotelmanagement.models;

import java.math.BigDecimal;
import java.time.Instant;

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
 * Pagos asociados a reservas. Refleja la tabla {@code pago}.
 * La FK la aplica la base de datos. {@code fecha_pago} tiene DEFAULT en BD:
 * con {@code @DynamicInsert} se omite si queda nula.
 */
@Entity
@Table(name = "pago")
@DynamicInsert
public class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pago", nullable = false)
	private Integer idPago;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_reserva", nullable = false)
	private Reserva reserva;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "tipo", nullable = false, columnDefinition = "tipo_movimiento_pago")
	private TipoMovimientoPago tipo;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "metodo", nullable = false, columnDefinition = "metodo_pago")
	private MetodoPago metodo;

	@Column(name = "fecha_pago", nullable = false)
	private Instant fechaPago;

	@Column(name = "monto", nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	public Pago() {
	}

	public Integer getIdPago() {
		return idPago;
	}

	public void setIdPago(Integer idPago) {
		this.idPago = idPago;
	}

	public Reserva getReserva() {
		return reserva;
	}

	public void setReserva(Reserva reserva) {
		this.reserva = reserva;
	}

	public TipoMovimientoPago getTipo() {
		return tipo;
	}

	public void setTipo(TipoMovimientoPago tipo) {
		this.tipo = tipo;
	}

	public MetodoPago getMetodo() {
		return metodo;
	}

	public void setMetodo(MetodoPago metodo) {
		this.metodo = metodo;
	}

	public Instant getFechaPago() {
		return fechaPago;
	}

	public void setFechaPago(Instant fechaPago) {
		this.fechaPago = fechaPago;
	}

	public BigDecimal getMonto() {
		return monto;
	}

	public void setMonto(BigDecimal monto) {
		this.monto = monto;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Pago other)) {
			return false;
		}
		return idPago != null && idPago.equals(other.idPago);
	}

	@Override
	public int hashCode() {
		return Pago.class.hashCode();
	}
}
