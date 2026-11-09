/**
 * @file rules.js
 * @description Reglas de negocio y validaciones de HostOps.
 * Pre-validaciones de UX equivalentes a constraints y triggers del esquema SQL;
 * el servidor sigue siendo la autoridad. Lee la caché DB pero no la muta.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, HOY, buscarPor } from './state.js';
import { TABLA } from './constants.js';

/**
 * Calcula número de noches entre dos fechas (entrada inclusive, salida exclusive)
 * @param {string} a - Fecha entrada YYYY-MM-DD
 * @param {string} b - Fecha salida YYYY-MM-DD
 * @returns {number}
 */
export const noches = (a, b) => (Date.parse(b) - Date.parse(a)) / 864e5;

/**
 * Verifica si dos rangos de fechas se solapan.
 * Los rangos son [inicio, fin): inicio inclusive, fin exclusive.
 * @param {string} a1 - Inicio rango 1
 * @param {string} b1 - Fin rango 1
 * @param {string} a2 - Inicio rango 2
 * @param {string} b2 - Fin rango 2
 * @returns {boolean}
 */
export const solapa = (a1, b1, a2, b2) => a1 < b2 && b1 > a2;

/**
 * Verifica conflictos de una habitación en un rango de fechas.
 * Comprueba solapamiento con reservas activas y con bloqueos de mantenimiento
 * usando el snapshot de disponibilidad cargado en la caché.
 * (ex = idDetalle a ignorar al editar una reserva existente)
 * @param {number} idHabitacion
 * @param {string} a - Fecha entrada
 * @param {string} b - Fecha salida
 * @param {number|null} ex - ID de detalle a excluir
 * @returns {string} Mensaje de error si hay conflicto, '' si no
 */
export const conflicto = (idHabitacion, a, b, ex) => {
  // Conflicto con otras reservas activas (no canceladas)
  if (
    DB[TABLA.det].some(
      (d) =>
        d.idHabitacion == idHabitacion &&
        d.estado != 'cancelada' &&
        d.idDetalle != ex &&
        a < d.fechaSalidaPrevista &&
        b > d.fechaEntradaPrevista,
    )
  )
    return 'se solapa con otra reserva activa';

  // Conflicto con bloqueos de mantenimiento
  if (
    DB[TABLA.bl].some(
      (x) => x.idHabitacion == idHabitacion && a <= x.fechaFin && b > x.fechaInicio,
    )
  )
    return 'está bloqueada por mantenimiento en esas fechas';

  return '';
};

/**
 * Valida UNA línea de reserva {h,a,b,o[]} frente a la caché y a las demás líneas
 * @param {Object} l - Línea con { h, a, b, o: [idsHuésped] }
 * @param {Object[]} otras - Otras líneas del mismo formulario
 * @param {Object} [opts] - Opciones: { ex, minA, minB }
 * @returns {string} Mensaje de error o '' si la línea es válida
 */
export const validarLinea = (l, otras, { ex = null, minA = null, minB = null } = {}) => {
  if (!l.h || !l.a || !l.b) return '';

  const h = buscarPor('habitacion', 'idHabitacion', l.h);
  if (!h) return 'Habitación no encontrada.';

  if (l.b <= l.a) return 'La salida debe ser posterior a la entrada.';
  if (minA && l.a < minA) return 'La entrada no puede ser anterior a hoy.';
  if (minB && l.b < minB) return 'La salida no puede ser anterior a hoy.';

  const c = conflicto(l.h, l.a, l.b, ex);
  if (c) return `La habitación ${h.numero} ${c}.`;

  // Conflictos con las demás líneas del mismo formulario
  const comp = otras.filter((x) => x.a && x.b && solapa(x.a, x.b, l.a, l.b));
  if (comp.some((x) => x.h == l.h))
    return `La habitación ${h.numero} ya está en otra línea con fechas que se cruzan.`;

  if (l.o.length > h.capacidad)
    return `Capacidad excedida: la habitación ${h.numero} admite ${h.capacidad} huésped(es) y seleccionaste ${l.o.length}.`;

  if (comp.some((x) => x.o.some((i) => l.o.includes(i))))
    return 'Un mismo huésped no puede estar en dos habitaciones con fechas que se cruzan.';

  return '';
};

/**
 * Valida si se puede hacer check-in en un detalle de reserva
 * (pre-validación de UX; el servidor también lo comprueba)
 * @param {Object} d - Detalle de reserva (fechaEntradaPrevista, estado)
 * @param {Object} h - Habitación asociada (estado, numero)
 * @returns {string} Mensaje de error o '' si el check-in es válido
 */
export const errorCheckin = (d, h) => {
  if (d.fechaEntradaPrevista > HOY)
    return `Aún no es la fecha de entrada (${d.fechaEntradaPrevista}).`;
  if (h.estado != 'disponible')
    return `La habitación ${h.numero} está en estado «${h.estado}»; debe estar disponible.`;
  return '';
};

/**
 * Objeto agregado con todas las reglas: interfaz principal usada por las vistas
 */
export const reglas = {
  noches,
  solapa,
  conflicto,
  validarLinea,
  errorCheckin,
};
