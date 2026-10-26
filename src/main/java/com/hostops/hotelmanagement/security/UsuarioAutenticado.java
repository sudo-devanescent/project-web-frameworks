package com.hostops.hotelmanagement.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal autenticado con los datos mínimos que necesita la API.
 */
public class UsuarioAutenticado implements UserDetails {

	private final Integer idUsuario;
	private final Integer idRol;
	private final String nombreUsuario;
	private final String nombreCompleto;
	private final String rol;
	private final String contrasenaHash;
	private final boolean activo;

	public UsuarioAutenticado(Integer idUsuario, Integer idRol, String nombreUsuario,
			String nombreCompleto, String rol, String contrasenaHash, boolean activo) {
		this.idUsuario = idUsuario;
		this.idRol = idRol;
		this.nombreUsuario = nombreUsuario;
		this.nombreCompleto = nombreCompleto;
		this.rol = rol;
		this.contrasenaHash = contrasenaHash;
		this.activo = activo;
	}

	public Integer getIdUsuario() {
		return idUsuario;
	}

	public Integer getIdRol() {
		return idRol;
	}

	public String getNombreCompleto() {
		return nombreCompleto;
	}

	public String getRol() {
		return rol;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(
				"ROLE_" + rol.toUpperCase(Locale.ROOT)));
	}

	@Override
	public String getPassword() {
		return contrasenaHash;
	}

	@Override
	public String getUsername() {
		return nombreUsuario;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return activo;
	}

	@Override
	public boolean equals(Object otro) {
		if (this == otro) {
			return true;
		}
		return otro instanceof UsuarioAutenticado usuario
				&& Objects.equals(idUsuario, usuario.idUsuario);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(idUsuario);
	}

}
