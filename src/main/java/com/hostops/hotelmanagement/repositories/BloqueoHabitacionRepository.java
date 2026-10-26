package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostops.hotelmanagement.models.BloqueoHabitacion;

import jakarta.persistence.LockModeType;

public interface BloqueoHabitacionRepository extends JpaRepository<BloqueoHabitacion, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from BloqueoHabitacion b where b.idBloqueo = :id")
	Optional<BloqueoHabitacion> findByIdForUpdate(@Param("id") Integer id);

	List<BloqueoHabitacion> findByHabitacion_IdHabitacionOrderByFechaInicioAsc(Integer idHabitacion);

	boolean existsByHabitacion_IdHabitacion(Integer idHabitacion);

}
