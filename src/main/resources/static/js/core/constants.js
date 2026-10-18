/**
 * @file constants.js
 * @description Constantes globales de la aplicación HostOps.
 * Define enums, listas estáticas, configuración de navegación y matriz de permisos.
 * @author HostOps Team
 * @version 1.0.0
 */

/* =====================================================================
   Constantes de dominio (equivalen a tablas de catálogo en BD)
   ===================================================================== */

/** Estados posibles de una habitación */
export const ESTH = ['disponible', 'ocupada', 'limpieza', 'mantenimiento'];

/** Tipos de habitación disponibles */
export const TIPOS = ['simple', 'doble', 'matrimonial', 'triple', 'suite'];

/** Métodos de pago aceptados */
export const METODOS = ['efectivo', 'tarjeta', 'transferencia', 'billetera_digital'];

/** Mapeo de estado -> clase de badge Bootstrap */
export const COLOR_BADGE = {
  disponible: 'success',
  ocupada: 'primary',
  limpieza: 'warning',
  mantenimiento: 'danger',
  confirmada: 'info',
  en_estadia: 'primary',
  finalizada: 'secondary',
  cancelada: 'danger',
  anticipo: 'info',
  saldo: 'success',
  reembolso: 'warning',
};

/* =====================================================================
   Navegación y permisos
   ===================================================================== */

/** Definición del menú de navegación: [hash, icono, etiqueta] */
export const NAV = [
  ['dash', 'speedometer2', 'Dashboard'],
  ['hab', 'door-closed', 'Habitaciones'],
  ['res', 'calendar-check', 'Reservas'],
  ['hue', 'people', 'Huéspedes'],
  ['fin', 'cash-coin', 'Finanzas'],
  ['cfg', 'gear', 'Configuración'],
];

/** Matriz de permisos por rol: vistas permitidas */
export const PERM = {
  administrador: ['dash', 'hab', 'res', 'hue', 'fin', 'cfg'],
  recepcionista: ['dash', 'hab', 'res', 'hue', 'fin'],
};

/** Aliases de nombres de tablas para evitar strings mágicos */
export const TABLA = {
  bl: 'bloqueo_habitacion',
  det: 'detalle_reserva',
  dh: 'detalle_huesped',
};