/**
 * @file index.js
 * @description Barrel del núcleo: re-exporta constantes, utilidades, estado,
 * reglas, cliente HTTP y catálogos. Permite imports limpios:
 * `import { DB, reglas, api } from './core/index.js'`
 * @author HostOps Team
 * @version 1.0.0
 */

export * from './constants.js';
export * from './utils.js';
export * from './state.js';
export * from './rules.js';
export * from './api.js';
export * from './catalogos.js';
