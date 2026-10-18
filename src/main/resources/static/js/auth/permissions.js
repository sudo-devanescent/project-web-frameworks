/**
 * @file permissions.js
 * @description Helpers de permisos y roles para HostOps.
 * Encapsula la lógica de autorización basada en la matriz PERM.
 * @author HostOps Team
 * @version 1.0.0
 */

import { PERM } from '../core/constants.js';
import { SES } from '../core/state.js';

/**
 * Verifica si la sesión actual tiene permiso para una vista/acción
 * @param {string} key - Clave de vista (dash, hab, res, hue, fin, cfg)
 * @returns {boolean}
 */
export const puede = (key) => !!SES && PERM[SES.rol]?.includes(key);

/**
 * Verifica si el usuario actual es administrador
 * @returns {boolean}
 */
export const esAdmin = () => SES?.rol === 'administrador';

/**
 * Obtiene el rol del usuario actual
 * @returns {string|null}
 */
export const getRol = () => SES?.rol ?? null;

/**
 * Obtiene el UID del usuario actual (para auditoría FK)
 * @returns {number|null}
 */
export const getUid = () => SES?.uid ?? null;

/**
 * Obtiene el nombre del usuario actual
 * @returns {string|null}
 */
export const getNombre = () => SES?.nombre ?? null;