package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostops.hotelmanagement.models.Reserva;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Reserva r where r.idReserva = :id")
	Optional<Reserva> findByIdForUpdate(@Param("id") Integer id);

	List<Reserva> findAllByOrderByFechaCreacionDesc();

	List<Reserva> findByHuespedTitular_IdHuespedOrderByFechaCreacionDesc(Integer idHuesped);

	boolean existsByHuespedTitular_IdHuesped(Integer idHuesped);

	boolean existsByCanal_IdCanal(Integer idCanal);

	boolean existsByUsuarioCreador_IdUsuario(Integer idUsuario);

	boolean existsByUsuarioModificador_IdUsuario(Integer idUsuario);

}
