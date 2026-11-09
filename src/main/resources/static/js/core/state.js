/**
 * @file state.js
 * @description Estado global de la aplicación HostOps.
 * Contiene la caché de datos del servidor (DB), la sesión activa, los filtros
 * y el registro de listados. Es el único módulo con estado mutable compartido.
 * @author HostOps Team
 * @version 1.0.0
 */

import { fechaRelativa } from './utils.js';

// === FECHAS DE REFERENCIA ===
/** Fecha de hoy en formato YYYY-MM-DD (se inicializa al cargar el módulo) */
export const HOY = fechaRelativa(0);

// === CACHÉ DE DATOS DEL SERVIDOR ===
/**
 * Caché de colecciones descargadas de la API (formas camelCase de los DTO).
 * Se rellena desde api.js/catalogos.js; nunca se generan registros localmente.
 * Se refresca en cada render de vista o tras cada mutación.
 * @type {Object.<string, Object[]>}
 */
export const DB = {
  rol: [],
  usuario: [],
  canal: [],
  habitacion: [],
  huesped: [],
  detalle_reserva: [],
  bloqueo_habitacion: [],
};

/**
 * Busca un registro en la caché DB por campo y valor (coerción ==)
 * @param {string} table - Nombre de la colección
 * @param {string} key - Campo a buscar
 * @param {any} value - Valor a comparar
 * @returns {Object|undefined}
 */
export const buscarPor = (table, key, value) => DB[table].find((x) => x[key] == value);

// === ESTADO DE SESIÓN Y UI ===

/**
 * Sesión activa: null si no hay login, { nombre, rol, uid } si autenticado.
 * Los módulos importados no pueden reasignar `let`, por lo que se exponen
 * setters (iniciarSesion/cerrarSesion) que actualizan el binding de forma
 * segura desde cualquier módulo.
 */
export let SES = null;

/**
 * Establece la sesión activa (login o restauración de sesión)
 * @param {Object} datos - { nombre, rol, uid }
 */
export function iniciarSesion(datos) {
  SES = datos;
}

/**
 * Limpia la sesión activa (logout)
 */
export function cerrarSesion() {
  SES = null;
}

/** Filtros activos por vista (persisten en memoria durante la sesión) */
export const filtros = {
  hab: { estado: '', tipo: '' },
  res: { q: '', estado: '' },
  hue: '',
};

/** Funciones de renderizado de listados por vista (se registran desde cada vista) */
export const listados = {};
