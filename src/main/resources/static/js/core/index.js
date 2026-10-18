/**
 * @file index.js
 * @description Barrel del núcleo: re-exporta constantes, utilidades, estado y reglas.
 * Permite imports limpios: `import { DB, reglas } from './core/index.js'`
 * @author HostOps Team
 * @version 1.0.0
 */

export * from './constants.js';
export * from './utils.js';
export * from './state.js';
export * from './rules.js';
