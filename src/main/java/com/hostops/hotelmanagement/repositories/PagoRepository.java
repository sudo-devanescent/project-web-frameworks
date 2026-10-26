package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostops.hotelmanagement.models.Pago;

import jakarta.persistence.LockModeType;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

	List<Pago> findByReserva_IdReservaOrderByFechaPagoAsc(Integer idReserva);

	Optional<Pago> findFirstByReserva_IdReservaOrderByFechaPagoDesc(Integer idReserva);

	boolean existsByReserva_IdReserva(Integer idReserva);

	boolean existsByPagoOriginal_IdPago(Integer idPago);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pago p where p.idPago = :id")
	Optional<Pago> findByIdForUpdate(@Param("id") Integer id);
}
