/**
 * @file utils.js
 * @description Utilidades generales de la aplicación HostOps.
 * Funciones puras, sin dependencias de estado (DB) ni del DOM más allá de qs.
 * @author HostOps Team
 * @version 1.0.0
 */

/**
 * Selector corto para document.querySelector
 * @param {string} s - Selector CSS
 * @returns {Element|null}
 */
export const qs = (s) => document.querySelector(s);

/**
 * Convierte Date a string ISO corto (YYYY-MM-DD) en zona horaria local
 * @param {Date} d
 * @returns {string}
 */
export const iso = (d) => d.toLocaleDateString('sv');

/**
 * Calcula fecha relativa a hoy (suma o resta días)
 * @param {number} n - Días a sumar (negativo para pasado)
 * @returns {string} Fecha en formato YYYY-MM-DD
 */
export const fechaRelativa = (n) => {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return iso(d);
};

/**
 * Timestamp ISO actual
 * @returns {string}
 */
export const ahora = () => new Date().toISOString();

/**
 * Formatea número como moneda peruana (Soles)
 * @param {number} n
 * @returns {string} Ej: "S/ 1,234.56"
 */
export const soles = (n) =>
  'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * Mapa de entidades HTML (nombre -> cuerpo de la entidad).
 * Se construye por concatenación para garantizar literales correctos.
 */
const ENTIDADES = { '&': 'amp', '<': 'lt', '>': 'gt', '"': 'quot' };

/**
 * Escapa caracteres HTML peligrosos para prevenir XSS
 * @param {string} s
 * @returns {string} Texto seguro para insertar en HTML
 */
export const esc = (s) =>
  String(s ?? '').replace(/[&<>"]/g, (c) => '&' + ENTIDADES[c] + ';');

/**
 * Convierte array de arrays a array de objetos usando las claves dadas
 * @param {string[]} keys - Nombres de propiedades
 * @param {any[][]} rows - Filas de datos
 * @returns {Object[]}
 */
export const filasDesdeArreglo = (keys, rows) =>
  rows.map((r) => Object.fromEntries(keys.map((k, i) => [k, r[i]])));

/**
 * Nombre completo de huésped a partir de su objeto
 * @param {Object|null} h - Objeto huésped con nombres y apellidos
 * @returns {string}
 */
export const nombreHuesped = (h) => (h ? h.nombres + ' ' + h.apellidos : '—');
