package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostops.hotelmanagement.models.DetalleReserva;

import jakarta.persistence.LockModeType;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DetalleReserva d where d.idDetalle = :id")
	Optional<DetalleReserva> findByIdForUpdate(@Param("id") Integer id);

	List<DetalleReserva> findByReserva_IdReservaOrderByFechaEntradaPrevistaAsc(Integer idReserva);

	boolean existsByReserva_IdReserva(Integer idReserva);

	boolean existsByHabitacion_IdHabitacion(Integer idHabitacion);

}
