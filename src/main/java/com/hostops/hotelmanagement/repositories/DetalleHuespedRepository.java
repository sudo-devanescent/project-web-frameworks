package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostops.hotelmanagement.models.DetalleHuesped;
import com.hostops.hotelmanagement.models.DetalleHuespedId;
import com.hostops.hotelmanagement.models.EstadoDetalle;

public interface DetalleHuespedRepository extends JpaRepository<DetalleHuesped, DetalleHuespedId> {

	List<DetalleHuesped> findByDetalleReserva_IdDetalle(Integer idDetalle);

	List<DetalleHuesped> findByDetalleReserva_Reserva_IdReserva(Integer idReserva);

	Optional<DetalleHuesped> findByDetalleReserva_IdDetalleAndHuesped_IdHuesped(
			Integer idDetalle, Integer idHuesped);

	long countByDetalleReserva_IdDetalle(Integer idDetalle);

	boolean existsByHuesped_IdHuesped(Integer idHuesped);

	boolean existsByDetalleReserva_Reserva_IdReserva(Integer idReserva);

	boolean existsByDetalleReserva_Reserva_IdReservaAndHuesped_IdHuesped(
			Integer idReserva, Integer idHuesped);

	@Query("""
			select count(dh) from DetalleHuesped dh
			 where dh.detalleReserva.habitacion.idHabitacion = :idHabitacion
			   and dh.detalleReserva.estado not in :estadosFinales
			 group by dh.detalleReserva.idDetalle
			""")
	List<Long> contarOcupantesPorDetalleActivo(
			@Param("idHabitacion") Integer idHabitacion,
			@Param("estadosFinales") List<EstadoDetalle> estadosFinales);

	void deleteByDetalleReserva_IdDetalle(Integer idDetalle);

}
