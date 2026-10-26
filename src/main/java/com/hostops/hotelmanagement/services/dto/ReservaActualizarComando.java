package com.hostops.hotelmanagement.services.dto;

public record ReservaActualizarComando(
		Integer idHuespedTitular,
		Integer idCanal,
		Integer idUsuarioModificador) {
}
