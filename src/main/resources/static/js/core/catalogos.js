/**
 * @file catalogos.js
 * @description Carga y refresco de colecciones de datos desde la API hacia la
 * caché DB (state.js): catálogos (roles, usuarios, canales, habitaciones y
 * huéspedes), detalles de reserva y bloqueos por habitación.
 * @author HostOps Team
 * @version 1.0.0
 */

import { api } from './api.js';
import { DB } from './state.js';

/** Rutas de listado por colección de catálogo */
const FUENTES = {
  rol: '/api/roles',
  usuario: '/api/usuarios',
  canal: '/api/canales',
  habitacion: '/api/habitaciones',
  huesped: '/api/huespedes',
};

/**
 * Descarga el catálogo indicado y lo guarda en la caché DB
 * @param {string} nombre - Clave de colección (rol, usuario, canal, habitacion, huesped)
 * @returns {Promise<Object[]>}
 */
export async function refrescarCatalogo(nombre) {
  const ruta = FUENTES[nombre];
  if (!ruta) throw new Error('Catálogo desconocido: ' + nombre);
  const datos = await api.get(ruta);
  DB[nombre] = datos;
  return datos;
}

/**
 * Descarga la lista completa de detalles de reserva hacia la caché DB
 * (se usa antes de validar conflictos de fechas en formularios)
 * @returns {Promise<Object[]>}
 */
export async function refrescarDetalles() {
  const datos = await api.get('/api/detalles-reserva');
  DB.detalle_reserva = datos;
  return datos;
}

/**
 * Descarga los bloqueos de todas las habitaciones hacia la caché DB
 * (se usa antes de validar conflictos con mantenimiento en formularios).
 * Requiere que el catálogo de habitaciones esté cargado.
 * @returns {Promise<Object[]>}
 */
export async function cargarBloqueos() {
  const listas = await Promise.all(
    DB.habitacion.map((h) => api.get('/api/habitaciones/' + h.idHabitacion + '/bloqueos')),
  );
  DB.bloqueo_habitacion = listas.flat();
  return DB.bloqueo_habitacion;
}
