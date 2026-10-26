package com.hostops.hotelmanagement.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostops.hotelmanagement.models.Canal;

public interface CanalRepository extends JpaRepository<Canal, Integer> {

	Optional<Canal> findByNombre(String nombre);

	boolean existsByNombre(String nombre);

	boolean existsByNombreAndIdCanalNot(String nombre, Integer idCanal);

}
