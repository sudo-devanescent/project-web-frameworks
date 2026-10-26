package com.hostops.hotelmanagement.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hostops.hotelmanagement.services.dto.NochesOcupadasPorMesDto;
import com.hostops.hotelmanagement.services.dto.SaldoReservaDto;

@Repository
public class VistasConsultaRepository {

	private final JdbcTemplate jdbcTemplate;

	public VistasConsultaRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<SaldoReservaDto> obtenerSaldoReserva(Integer idReserva) {
		List<SaldoReservaDto> resultados = jdbcTemplate.query("""
				select id_reserva, total_alojamiento, total_penalidades, total_a_pagar,
				       cobrado_neto, saldo_pendiente, reembolso_pendiente
				  from v_saldo_reserva
				 where id_reserva = ?
				""", (rs, fila) -> new SaldoReservaDto(
						rs.getInt("id_reserva"),
						rs.getBigDecimal("total_alojamiento"),
						rs.getBigDecimal("total_penalidades"),
						rs.getBigDecimal("total_a_pagar"),
						rs.getBigDecimal("cobrado_neto"),
						rs.getBigDecimal("saldo_pendiente"),
						rs.getBigDecimal("reembolso_pendiente")), idReserva);
		return resultados.stream().findFirst();
	}

	public List<NochesOcupadasPorMesDto> listarNochesOcupadasPorMes() {
		return jdbcTemplate.query("""
				select mes, noches_ocupadas
				  from v_noches_ocupadas_por_mes
				 order by mes
				""", (rs, fila) -> new NochesOcupadasPorMesDto(
						rs.getDate("mes").toLocalDate(),
						rs.getLong("noches_ocupadas")));
	}

}
