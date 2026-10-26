package com.hostops.hotelmanagement.services.dto;

import java.math.BigDecimal;

public record CancelacionComando(
		String motivo,
		BigDecimal penalidad,
		Integer idUsuarioModificador) {
}
