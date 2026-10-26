package com.hostops.hotelmanagement.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;

/**
 * Carga los usuarios activos para la autenticación por sesión.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
				.filter(u -> Boolean.TRUE.equals(u.getActivo()))
				.orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));
		return new UsuarioAutenticado(
				usuario.getIdUsuario(),
				usuario.getRol().getIdRol(),
				usuario.getNombreUsuario(),
				usuario.getNombreCompleto(),
				usuario.getRol().getNombre(),
				usuario.getContrasenaHash(),
				Boolean.TRUE.equals(usuario.getActivo()));
	}

}
