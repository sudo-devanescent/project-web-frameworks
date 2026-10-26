package com.hostops.hotelmanagement.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostops.hotelmanagement.models.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {

	Optional<Rol> findByNombre(String nombre);

	boolean existsByNombre(String nombre);

	boolean existsByNombreAndIdRolNot(String nombre, Integer idRol);

}
