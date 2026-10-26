package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostops.hotelmanagement.models.Usuario;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.idUsuario = :id")
	Optional<Usuario> findByIdForUpdate(@Param("id") Integer id);

	Optional<Usuario> findByNombreUsuario(String nombreUsuario);

	boolean existsByNombreUsuario(String nombreUsuario);

	boolean existsByCorreo(String correo);

	boolean existsByRol_IdRol(Integer idRol);

	boolean existsByNombreUsuarioAndIdUsuarioNot(String nombreUsuario, Integer idUsuario);

	boolean existsByCorreoAndIdUsuarioNot(String correo, Integer idUsuario);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.rol.nombre = :nombre and u.activo = true order by u.idUsuario")
	List<Usuario> findActivosByRolNombreForUpdate(@Param("nombre") String nombreRol);

	long countByRol_NombreAndActivoTrue(String nombreRol);

}
