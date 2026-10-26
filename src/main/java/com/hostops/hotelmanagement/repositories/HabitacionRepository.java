package com.hostops.hotelmanagement.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostops.hotelmanagement.models.Habitacion;

import jakarta.persistence.LockModeType;

public interface HabitacionRepository extends JpaRepository<Habitacion, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select h from Habitacion h where h.idHabitacion = :id")
	Optional<Habitacion> findByIdForUpdate(@Param("id") Integer id);

	Optional<Habitacion> findByNumero(String numero);

	boolean existsByNumero(String numero);

	boolean existsByNumeroAndIdHabitacionNot(String numero, Integer idHabitacion);

}
