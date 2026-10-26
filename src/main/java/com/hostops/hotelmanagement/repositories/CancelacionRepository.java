package com.hostops.hotelmanagement.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostops.hotelmanagement.models.Cancelacion;

public interface CancelacionRepository extends JpaRepository<Cancelacion, Integer> {

	Optional<Cancelacion> findByDetalleReserva_IdDetalle(Integer idDetalle);

	boolean existsByDetalleReserva_IdDetalle(Integer idDetalle);

}
