package com.hostops.hotelmanagement.security;

/**
 * Evento publicado cuando deben revocarse las sesiones de un usuario.
 */
public record SesionRevocadaEvent(Integer idUsuario) {
}
