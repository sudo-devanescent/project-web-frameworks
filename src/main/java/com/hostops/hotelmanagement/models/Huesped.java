package com.hostops.hotelmanagement.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Huéspedes del hotel. Refleja la tabla {@code huesped}.
 * La restricción UNIQUE (tipo_documento, numero_documento) la aplica la BD.
 */
@Entity
@Table(name = "huesped")
public class Huesped {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_huesped", nullable = false)
	private Integer idHuesped;

	@Column(name = "tipo_documento", nullable = false, length = 20)
	private String tipoDocumento;

	@Column(name = "numero_documento", nullable = false, length = 30)
	private String numeroDocumento;

	@Column(name = "nombres", nullable = false, length = 100)
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = 100)
	private String apellidos;

	@Column(name = "telefono", nullable = false, length = 30)
	private String telefono;

	@Column(name = "correo", length = 120)
	private String correo;

	@Column(name = "nacionalidad", nullable = false, length = 60)
	private String nacionalidad;

	@Column(name = "fecha_nacimiento", nullable = false)
	private LocalDate fechaNacimiento;

	@Column(name = "preferencias")
	private String preferencias;

	public Huesped() {
	}

	public Integer getIdHuesped() {
		return idHuesped;
	}

	public void setIdHuesped(Integer idHuesped) {
		this.idHuesped = idHuesped;
	}

	public String getTipoDocumento() {
		return tipoDocumento;
	}

	public void setTipoDocumento(String tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}

	public String getNumeroDocumento() {
		return numeroDocumento;
	}

	public void setNumeroDocumento(String numeroDocumento) {
		this.numeroDocumento = numeroDocumento;
	}

	public String getNombres() {
		return nombres;
	}

	public void setNombres(String nombres) {
		this.nombres = nombres;
	}

	public String getApellidos() {
		return apellidos;
	}

	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getNacionalidad() {
		return nacionalidad;
	}

	public void setNacionalidad(String nacionalidad) {
		this.nacionalidad = nacionalidad;
	}

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public void setFechaNacimiento(LocalDate fechaNacimiento) {
		this.fechaNacimiento = fechaNacimiento;
	}

	public String getPreferencias() {
		return preferencias;
	}

	public void setPreferencias(String preferencias) {
		this.preferencias = preferencias;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Huesped other)) {
			return false;
		}
		return idHuesped != null && idHuesped.equals(other.idHuesped);
	}

	@Override
	public int hashCode() {
		return Huesped.class.hashCode();
	}
}
