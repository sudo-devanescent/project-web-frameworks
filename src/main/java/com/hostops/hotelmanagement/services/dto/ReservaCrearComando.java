package com.hostops.hotelmanagement.services.dto;

public record ReservaCrearComando(
		Integer idHuespedTitular,
		Integer idCanal,
		Integer idUsuarioCreador) {
}
