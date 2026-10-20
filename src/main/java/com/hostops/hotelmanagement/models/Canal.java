package com.hostops.hotelmanagement.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Canales de captación de reservas. Refleja la tabla {@code canal}.
 * La restricción UNIQUE la aplica la base de datos.
 */
@Entity
@Table(name = "canal")
public class Canal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_canal", nullable = false)
	private Integer idCanal;

	@Column(name = "nombre", nullable = false, length = 50, unique = true)
	private String nombre;

	public Canal() {
	}

	public Integer getIdCanal() {
		return idCanal;
	}

	public void setIdCanal(Integer idCanal) {
		this.idCanal = idCanal;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Canal other)) {
			return false;
		}
		return idCanal != null && idCanal.equals(other.idCanal);
	}

	@Override
	public int hashCode() {
		return Canal.class.hashCode();
	}
}
