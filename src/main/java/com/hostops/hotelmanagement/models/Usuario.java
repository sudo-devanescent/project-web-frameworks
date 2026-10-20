package com.hostops.hotelmanagement.models;

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
 * Empleados que operan el sistema. Refleja la tabla {@code usuario}.
 * Las restricciones UNIQUE y la FK las aplica la base de datos.
 * {@code activo} tiene DEFAULT en BD: con {@code @DynamicInsert} se omite si
 * queda nulo y lo rellena la BD.
 */
@Entity
@Table(name = "usuario")
@DynamicInsert
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario", nullable = false)
	private Integer idUsuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_rol", nullable = false)
	private Rol rol;

	@Column(name = "nombre_usuario", nullable = false, length = 50, unique = true)
	private String nombreUsuario;

	@Column(name = "nombre_completo", nullable = false, length = 120)
	private String nombreCompleto;

	@Column(name = "correo", nullable = false, length = 120, unique = true)
	private String correo;

	@Column(name = "contrasena_hash", nullable = false, length = 255)
	private String contrasenaHash;

	@Column(name = "activo", nullable = false)
	private Boolean activo;

	public Usuario() {
	}

	public Integer getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Integer idUsuario) {
		this.idUsuario = idUsuario;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

	public String getNombreUsuario() {
		return nombreUsuario;
	}

	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	public String getNombreCompleto() {
		return nombreCompleto;
	}

	public void setNombreCompleto(String nombreCompleto) {
		this.nombreCompleto = nombreCompleto;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getContrasenaHash() {
		return contrasenaHash;
	}

	public void setContrasenaHash(String contrasenaHash) {
		this.contrasenaHash = contrasenaHash;
	}

	public Boolean getActivo() {
		return activo;
	}

	public void setActivo(Boolean activo) {
		this.activo = activo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Usuario other)) {
			return false;
		}
		return idUsuario != null && idUsuario.equals(other.idUsuario);
	}

	@Override
	public int hashCode() {
		return Usuario.class.hashCode();
	}
}
