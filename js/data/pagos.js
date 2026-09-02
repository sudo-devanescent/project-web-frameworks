/* ============================================================
   DATOS: PAGOS
   Resumen de pagos / reservas por estado de pago. Al ser un
   prototipo, los datos están hardcodeados; en el sistema real
   se obtendrían del backend.
   ============================================================= */

/**
 * Lista de pagos por estado.
 * @type {Array<{reserva: string, huesped: string, monto: string,
 *               estado: string, fecha: string}>}
 */
export const pagos = [
    { reserva: '101', huesped: 'Juan Pérez',   monto: '$120', estado: 'Pendiente', fecha: '2026-01-15' },
    { reserva: '103', huesped: 'María García', monto: '$90',  estado: 'Pagado',    fecha: '2026-01-10' }
];
