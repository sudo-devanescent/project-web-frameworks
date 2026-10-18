/**
 * @file state.js
 * @description Estado global de la aplicación HostOps.
 * Contiene la base de datos en memoria (DB), sesión activa, filtros y listados.
 * Es el único módulo con estado mutable compartido.
 * @author HostOps Team
 * @version 1.0.0
 */

import { filasDesdeArreglo, fechaRelativa } from './utils.js';

// === FECHAS DE REFERENCIA ===
/** Fecha de hoy en formato YYYY-MM-DD (se inicializa al cargar el módulo) */
export const HOY = fechaRelativa(0);

// === BASE DE DATOS EN MEMORIA (espejo de esquema PostgreSQL) ===
/**
 * @type {Object.<string, Object[]>}
 * Cada clave es el nombre de una tabla, valor es array de objetos (registros).
 * Los IDs son numéricos autoincrementales.
 */
export const DB = {
  rol: filasDesdeArreglo(
    ['id_rol', 'nombre'],
    [
      [1, 'administrador'],
      [2, 'recepcionista'],
    ],
  ),
  usuario: filasDesdeArreglo(
    ['id_usuario', 'id_rol', 'nombre_usuario', 'nombre_completo', 'correo', 'activo'],
    [
      [1, 1, 'admin', 'Carla Mendoza Ruiz', 'carla@hostops.pe', true],
      [2, 2, 'recep1', 'Luis Quispe Rojas', 'luis@hostops.pe', true],
      [3, 2, 'recep2', 'Ana Torres Vega', 'ana@hostops.pe', false],
    ],
  ),
  canal: filasDesdeArreglo(
    ['id_canal', 'nombre'],
    [
      [1, 'presencial'],
      [2, 'telefono'],
      [3, 'whatsapp'],
    ],
  ),
  habitacion: filasDesdeArreglo(
    ['id_habitacion', 'numero', 'tipo', 'capacidad', 'tarifa', 'estado'],
    [
      [1, '101', 'simple', 1, 120, 'limpieza'],
      [2, '102', 'doble', 2, 160, 'ocupada'],
      [3, '103', 'matrimonial', 2, 180, 'disponible'],
      [4, '104', 'triple', 3, 220, 'disponible'],
      [5, '105', 'suite', 4, 350, 'ocupada'],
      [6, '201', 'simple', 1, 120, 'disponible'],
      [7, '202', 'doble', 2, 160, 'disponible'],
      [8, '203', 'matrimonial', 2, 180, 'disponible'],
      [9, '204', 'triple', 3, 220, 'mantenimiento'],
      [10, '205', 'suite', 4, 380, 'disponible'],
    ],
  ),
  bloqueo_habitacion: filasDesdeArreglo(
    ['id_bloqueo', 'id_habitacion', 'fecha_inicio', 'fecha_fin', 'motivo'],
    [
      [1, 9, fechaRelativa(-1), fechaRelativa(4), 'Reparación de tubería'],
      [2, 8, fechaRelativa(10), fechaRelativa(12), 'Pintura de paredes'],
    ],
  ),
  huesped: filasDesdeArreglo(
    [
      'id_huesped',
      'tipo_documento',
      'numero_documento',
      'nombres',
      'apellidos',
      'telefono',
      'correo',
      'nacionalidad',
      'fecha_nacimiento',
      'preferencias',
    ],
    [
      [
        1,
        'DNI',
        '45872136',
        'Rosa',
        'Quispe Mamani',
        '987654321',
        'rosa.quispe@mail.com',
        'Perú',
        '1988-03-14',
        'Habitación silenciosa y almohada extra',
      ],
      [
        2,
        'pasaporte',
        'X4471826',
        'Daniel',
        'Whitfield',
        '+44 7700 900123',
        'd.whitfield@mail.co.uk',
        'Reino Unido',
        '1975-09-02',
        'Desayuno temprano; piso alto',
      ],
      [3, 'DNI', '70231894', 'Mariana', 'Paredes Soto', '912345678', '', 'Perú', '1993-11-27', ''],
      [
        4,
        'CE',
        '002458713',
        'Carlos',
        'Mendes Rocha',
        '955112233',
        'cmendes@mail.com',
        'Brasil',
        '1982-06-30',
        'Cama extra',
      ],
      [
        5,
        'DNI',
        '41209875',
        'Jorge',
        'Salas Ccori',
        '998877665',
        '',
        'Perú',
        '1979-01-19',
        'Factura a nombre de empresa',
      ],
      [
        6,
        'pasaporte',
        'AB123456',
        'Sophie',
        'Laurent',
        '933221100',
        'sophie@mail.fr',
        'Francia',
        '1990-07-08',
        '',
      ],
    ],
  ),
  reserva: filasDesdeArreglo(
    [
      'id_reserva',
      'id_huesped_titular',
      'id_canal',
      'id_usuario_creador',
      'id_usuario_modificador',
      'fecha_creacion',
      'fecha_modificacion',
    ],
    [
      [1, 1, 1, 1, null, fechaRelativa(-3) + 'T10:00:00', null],
      [2, 2, 2, 2, null, fechaRelativa(-2) + 'T10:00:00', null],
      [3, 3, 3, 2, null, fechaRelativa(-1) + 'T10:00:00', null],
      [4, 4, 2, 1, null, fechaRelativa(-1) + 'T11:00:00', null],
      [5, 5, 1, 2, null, fechaRelativa(-8) + 'T10:00:00', null],
      [6, 6, 3, 1, null, fechaRelativa(-2) + 'T09:00:00', null],
    ],
  ),
  detalle_reserva: filasDesdeArreglo(
    [
      'id_detalle',
      'id_reserva',
      'id_habitacion',
      'fecha_entrada_prevista',
      'fecha_salida_prevista',
      'checkin_real',
      'checkout_real',
      'tarifa_noche',
      'estado',
    ],
    [
      [
        1,
        1,
        2,
        fechaRelativa(-2),
        fechaRelativa(2),
        fechaRelativa(-2) + 'T14:10:00',
        null,
        160,
        'en_estadia',
      ],
      [
        2,
        2,
        5,
        fechaRelativa(-1),
        fechaRelativa(3),
        fechaRelativa(-1) + 'T15:30:00',
        null,
        350,
        'en_estadia',
      ],
      [3, 3, 3, fechaRelativa(0), fechaRelativa(2), null, null, 180, 'confirmada'],
      [4, 4, 6, fechaRelativa(3), fechaRelativa(6), null, null, 120, 'confirmada'],
      [
        5,
        5,
        4,
        fechaRelativa(-6),
        fechaRelativa(-3),
        fechaRelativa(-6) + 'T13:00:00',
        fechaRelativa(-3) + 'T11:00:00',
        220,
        'finalizada',
      ],
      [6, 6, 7, fechaRelativa(1), fechaRelativa(3), null, null, 160, 'cancelada'],
    ],
  ),
  detalle_huesped: filasDesdeArreglo(
    ['id_detalle', 'id_huesped'],
    [
      [1, 1],
      [2, 2],
      [2, 6],
      [3, 3],
      [4, 4],
      [5, 5],
      [6, 6],
    ],
  ),
  cancelacion: filasDesdeArreglo(
    ['id_cancelacion', 'id_detalle', 'fecha_cancelacion', 'motivo', 'penalidad'],
    [[1, 6, fechaRelativa(-1) + 'T09:00:00', 'Cambio de planes del huésped', 80]],
  ),
  pago: filasDesdeArreglo(
    ['id_pago', 'id_reserva', 'tipo', 'metodo', 'fecha_pago', 'monto'],
    [
      [1, 1, 'anticipo', 'efectivo', fechaRelativa(-2) + 'T14:00:00', 320],
      [2, 2, 'anticipo', 'tarjeta', fechaRelativa(-1) + 'T15:00:00', 700],
      [3, 3, 'anticipo', 'billetera_digital', fechaRelativa(-1) + 'T18:00:00', 180],
      [4, 5, 'anticipo', 'transferencia', fechaRelativa(-7) + 'T10:00:00', 300],
      [5, 5, 'saldo', 'efectivo', fechaRelativa(-3) + 'T11:00:00', 360],
      [6, 6, 'anticipo', 'transferencia', fechaRelativa(-2) + 'T10:00:00', 160],
    ],
  ),
};

// === ACCESORES DE DB (solo en este módulo se conoce el objeto DB) ===

/**
 * Genera siguiente ID autoincremental para una tabla
 * @param {string} table - Nombre de la tabla en DB
 * @param {string} key - Campo ID
 * @returns {number}
 */
export const siguienteId = (table, key) => Math.max(0, ...DB[table].map((x) => x[key])) + 1;

/**
 * Busca un registro en DB por campo y valor (coerción ==)
 * @param {string} table - Nombre de la tabla
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
 * Establece la sesión activa (login exitoso)
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