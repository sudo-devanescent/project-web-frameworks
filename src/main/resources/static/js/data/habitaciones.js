/* ============================================================
   DATOS: HABITACIONES
   Catálogo estático de habitaciones del hotel. En el sistema
   real, estos datos provendrían de una API o base de datos;
   aquí se mantienen hardcodeados para el prototipo.
   ============================================================= */

/**
 * Lista de habitaciones.
 * @type {Array<{numero: string, tipo: string, capacidad: string,
 *               precio: string, estado: string}>}
 */
export const habitaciones = [
    { numero: '101', tipo: 'Individual', capacidad: '1 persona', precio: '$35.00', estado: 'Disponible' },
    { numero: '102', tipo: 'Doble',      capacidad: '2 personas', precio: '$50.00', estado: 'Ocupada' },
    { numero: '103', tipo: 'Suite',      capacidad: '4 personas', precio: '$90.00', estado: 'Disponible' },
    { numero: '104', tipo: 'Doble',      capacidad: '2 personas', precio: '$50.00', estado: 'Mantenimiento' },
    { numero: '105', tipo: 'Individual', capacidad: '1 persona', precio: '$35.00', estado: 'Disponible' }
];
