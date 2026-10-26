package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostops.hotelmanagement.models.Huesped;

public interface HuespedRepository extends JpaRepository<Huesped, Integer> {

	Optional<Huesped> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

	boolean existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

	boolean existsByTipoDocumentoAndNumeroDocumentoAndIdHuespedNot(
			String tipoDocumento, String numeroDocumento, Integer idHuesped);

	List<Huesped> findByApellidosContainingIgnoreCaseOrNombresContainingIgnoreCaseOrderByApellidosAscNombresAsc(
			String apellidos, String nombres);

}
