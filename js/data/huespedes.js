/* ============================================================
   DATOS: HUÉSPEDES
   Lista de huéspedes del hotel. Actualmente vacía: las filas de
   la tabla de huéspedes se poblarán dinámicamente o desde el
   backend en una etapa posterior del proyecto.
   ============================================================= */

/**
 * Lista de huéspedes.
 * @type {Array<{numero: string, nombre: string, documento: string,
 *               fechaEntrada: string, estado: string}>}
 */
export const huespedes = [
    { numero: '1',  nombre: 'Juan Pérez',   documento: '—', fechaEntrada: '2026-01-15', estado: 'Pendiente' },
    { numero: '2',  nombre: 'María García', documento: '—', fechaEntrada: '2026-01-10', estado: 'Activo' }
];
