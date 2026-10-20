package com.hostops.hotelmanagement.models;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Perfiles de acceso del personal. Refleja la tabla {@code rol}.
 * Las restricciones UNIQUE y FK las aplica la base de datos.
 */
@Entity
@Table(name = "rol")
public class Rol {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_rol", nullable = false)
	private Integer idRol;

	@Column(name = "nombre", nullable = false, length = 50, unique = true)
	private String nombre;

	public Rol() {
	}

	public Integer getIdRol() {
		return idRol;
	}

	public void setIdRol(Integer idRol) {
		this.idRol = idRol;
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
		if (!(o instanceof Rol other)) {
			return false;
		}
		return idRol != null && idRol.equals(other.idRol);
	}

	@Override
	public int hashCode() {
		return Rol.class.hashCode();
	}
}
